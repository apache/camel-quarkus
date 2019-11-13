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

import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;

import org.apache.camel.component.xslt.XsltComponent;
import org.apache.camel.component.xslt.XsltUriResolver;
import org.apache.camel.quarkus.component.xslt.CamelXsltConfig;
import org.apache.camel.quarkus.component.xslt.CamelXsltErrorListener;
import org.apache.camel.quarkus.component.xslt.CamelXsltRecorder;
import org.apache.camel.quarkus.core.CamelServiceFilter;
import org.apache.camel.quarkus.core.FastCamelContext;
import org.apache.camel.quarkus.core.deployment.CamelBeanBuildItem;
import org.apache.camel.quarkus.core.deployment.CamelServiceFilterBuildItem;
import org.apache.camel.util.FileUtil;
import org.apache.camel.util.StringHelper;
import org.apache.commons.lang3.StringUtils;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;

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
    CamelBeanBuildItem xsltComponent(CamelXsltRecorder recorder, CamelXsltConfig config) {
        return new CamelBeanBuildItem(
                "xslt",
                XsltComponent.class.getName(),
                recorder.createXsltComponent(config));
    }

    @BuildStep
    void xsltResources(
            CamelXsltConfig config,
            BuildProducer<XsltGeneratedClassBuildItem> generatedNames,
            BuildProducer<GeneratedClassBuildItem> generatedClasses) throws Exception {

        Path destination = Files.createTempDirectory(XsltFeature.FEATURE);

        try {
            for (String source : config.sources) {
                final String name = FileUtil.stripExt(source, true);

                try {
                    TransformerFactory tf = TransformerFactory.newInstance();
                    tf.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
                    tf.setAttribute("generate-translet", true);
                    tf.setAttribute("translet-name", StringHelper.capitalize(name, true));
                    tf.setAttribute("package-name", config.packageName);
                    tf.setAttribute("destination-directory", destination.toString());
                    tf.setErrorListener(new CamelXsltErrorListener());
                    tf.newTemplates(new XsltUriResolver(new FastCamelContext(), source).resolve(source, null));
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
