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
import java.util.Set;

import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ArchiveRootBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import org.apache.camel.component.xslt.XsltComponent;
import org.apache.camel.quarkus.component.xslt.deployment.BuildTimeUriResolver.ResolutionResult;
import org.apache.camel.quarkus.component.xslt.CamelXsltConfig;
import org.apache.camel.quarkus.component.xslt.CamelXsltErrorListener;
import org.apache.camel.quarkus.component.xslt.CamelXsltRecorder;
import org.apache.camel.quarkus.component.xslt.RuntimeUriResolver.Builder;
import org.apache.camel.quarkus.core.CamelServiceFilter;
import org.apache.camel.quarkus.core.deployment.CamelBeanBuildItem;
import org.apache.camel.quarkus.core.deployment.CamelServiceFilterBuildItem;
import org.apache.camel.quarkus.support.xalan.XalanSupport;
import org.apache.commons.lang3.StringUtils;

import io.quarkus.runtime.RuntimeValue;

class XsltProcessor {
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

    @BuildStep
    void xsltResources(
            CamelXsltConfig config,
            BuildProducer<XsltGeneratedClassBuildItem> generatedNames,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<UriResolverEntryBuildItem> uriResolverEntries,
            ArchiveRootBuildItem archiveRoot) throws Exception {

        Path destination = Files.createTempDirectory(XsltFeature.FEATURE);
        final Set<String> translets = new LinkedHashSet<>();
        try {
            // TODO: figure out a better way to get the baseDir, see https://github.com/apache/camel-quarkus/issues/439
            final Path baseDir = archiveRoot.getArchiveRoot().getParent().getParent();
            final BuildTimeUriResolver resolver = new BuildTimeUriResolver(baseDir);
            for (String uri : config.sources) {
                ResolutionResult resolvedUri = resolver.resolve(uri);
                uriResolverEntries.produce(resolvedUri.toBuildItem());
                final String translet = resolvedUri.transletClassName;
                if (translets.contains(translet)) {
                    throw new RuntimeException("XSLT translet name clash: cannot add '" + translet
                            + "' to previously added translets " + translets);
                }
                try {
                    TransformerFactory tf = XalanSupport.newTransformerFactoryInstance();
                    tf.setAttribute("generate-translet", true);
                    tf.setAttribute("translet-name", translet);
                    tf.setAttribute("package-name", config.packageName);
                    tf.setAttribute("destination-directory", destination.toString());
                    tf.setErrorListener(new CamelXsltErrorListener());
                    tf.newTemplates(resolvedUri.source);
                } catch (TransformerException e) {
                    throw new RuntimeException(e);
                }
            }

            Files.walk(destination)
                    .sorted(Comparator.reverseOrder())
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".class"))
                    .forEach(path -> {
                        try {
                            final Path rel = destination.relativize(path);
                            final String fqcn = StringUtils.removeEnd(rel.toString(), ".class").replace('/', '.');
                            final byte[] data = Files.readAllBytes(path);

                            generatedClasses.produce(new GeneratedClassBuildItem(false, fqcn, data));
                            generatedNames.produce(new XsltGeneratedClassBuildItem(fqcn));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } finally {
            Files.walk(destination)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }
}
