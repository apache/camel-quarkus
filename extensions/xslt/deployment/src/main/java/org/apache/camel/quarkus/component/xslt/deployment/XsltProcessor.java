/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.quarkus.component.xslt.deployment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.quarkus.runtime.RuntimeValue;
import org.apache.camel.component.xslt.XsltComponent;
import org.apache.camel.quarkus.component.xslt.CamelXsltConfig;
import org.apache.camel.quarkus.component.xslt.CamelXsltErrorListener;
import org.apache.camel.quarkus.component.xslt.CamelXsltRecorder;
import org.apache.camel.quarkus.component.xslt.CamelXsltTransformerFactory;
import org.apache.camel.quarkus.component.xslt.RuntimeUriResolver.Builder;
import org.apache.camel.quarkus.component.xslt.deployment.BuildTimeUriResolver.ResolutionResult;
import org.apache.camel.quarkus.core.deployment.spi.CamelBeanBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelServiceFilter;
import org.apache.camel.quarkus.core.deployment.spi.CamelServiceFilterBuildItem;
import org.apache.commons.lang3.Strings;

class XsltProcessor {
    /** The name that JDKs affected by JDK-8344925 give to every translet, whatever name was asked for */
    private static final String IGNORED_TRANSLET_NAME = "die_verwandlung";

    /*
     * The xslt component is programmatically configured by the extension thus
     * we can safely prevent camel to instantiate a default instance.
     */
    @BuildStep
    CamelServiceFilterBuildItem serviceFilter() {
        return new CamelServiceFilterBuildItem(CamelServiceFilter.forComponent("xslt"));
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    CamelBeanBuildItem xsltComponent(
            CamelXsltRecorder recorder,
            CamelXsltConfig config,
            List<UriResolverEntryBuildItem> uriResolverEntries) {

        final RuntimeValue<Builder> builder = recorder.createRuntimeUriResolverBuilder();
        for (UriResolverEntryBuildItem entry : uriResolverEntries) {
            recorder.addRuntimeUriResolverEntry(
                    builder,
                    entry.getTemplateUri(),
                    entry.getTransletClassName());
        }

        return new CamelBeanBuildItem(
                "xslt",
                XsltComponent.class.getName(),
                recorder.createXsltComponent(config, builder));
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void xsltResources(
            CamelXsltConfig config,
            BuildProducer<XsltGeneratedClassBuildItem> generatedNames,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<UriResolverEntryBuildItem> uriResolverEntries) throws Exception {

        final Path destination = Files.createTempDirectory(XsltFeature.FEATURE);
        final Set<String> translets = new LinkedHashSet<>();
        try {
            final BuildTimeUriResolver resolver = new BuildTimeUriResolver();
            for (String uri : config.sources().orElse(List.of())) {
                ResolutionResult resolvedUri = resolver.resolve(uri);
                // Each template is compiled to a directory of its own so that its classes can be told apart from the rest
                final Path transletDirectory = Files.createDirectory(destination.resolve(String.valueOf(translets.size())));

                try {
                    TransformerFactory tf = new CamelXsltTransformerFactory();

                    for (Map.Entry<String, Boolean> entry : config.features().entrySet()) {
                        tf.setFeature(entry.getKey(), entry.getValue());
                    }

                    tf.setAttribute("generate-translet", true);
                    tf.setAttribute("translet-name", resolvedUri.transletClassName);
                    tf.setAttribute("package-name", config.packageName());
                    tf.setAttribute("destination-directory", transletDirectory.toString());
                    tf.setErrorListener(new CamelXsltErrorListener());
                    tf.setURIResolver(resolver);
                    tf.newTemplates(resolvedUri.source);
                } catch (TransformerException e) {
                    throw new RuntimeException("Could not compile XSLT " + uri, e);
                }

                final Map<String, byte[]> classes = readClasses(transletDirectory);
                // The name of the translet is read back rather than taken from what was asked for, as the compiler adapts
                // it to a valid class name. Auxiliary classes are named after the translet with a $ suffix.
                final String translet = classes.keySet().stream()
                        .min(Comparator.comparingInt(String::length))
                        .orElseThrow(() -> new RuntimeException("No translet was generated for XSLT " + uri));
                final String transletName = translet.substring(translet.lastIndexOf('.') + 1);

                if (!translets.add(translet)) {
                    String message = "XSLT translet name clash: cannot add '" + translet + "' for " + uri
                            + " to previously added translets " + translets;
                    if (IGNORED_TRANSLET_NAME.equals(transletName)) {
                        message += ". The JDK running the build names every translet '" + IGNORED_TRANSLET_NAME
                                + "' (JDK-8344925), so only one template can be compiled. Build with JDK 21.0.8 or newer";
                    }
                    throw new RuntimeException(message);
                }

                uriResolverEntries.produce(new UriResolverEntryBuildItem(resolvedUri.templateUri, transletName));
                classes.forEach((fqcn, data) -> {
                    generatedClasses.produce(new GeneratedClassBuildItem(false, fqcn, data));
                    generatedNames.produce(new XsltGeneratedClassBuildItem(fqcn));
                });
            }
        } finally {
            try (Stream<Path> files = Files.walk(destination)) {
                files
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        }
    }

    private static Map<String, byte[]> readClasses(Path directory) throws IOException {
        final List<Path> classFiles;
        try (Stream<Path> files = Files.walk(directory)) {
            classFiles = files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".class"))
                    .toList();
        }

        final Map<String, byte[]> classes = new TreeMap<>();
        for (Path path : classFiles) {
            final Path rel = directory.relativize(path);
            final String fqcn = Strings.CI.removeEnd(rel.toString(), ".class").replace(File.separatorChar, '.');
            classes.put(fqcn, Files.readAllBytes(path));
        }
        return classes;
    }
}
