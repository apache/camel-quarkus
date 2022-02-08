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
package org.apache.camel.quarkus.component.xslt;

import java.util.Map;

import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.component.xslt.TransformerFactoryConfigurationStrategy;
import org.apache.camel.component.xslt.XsltComponent;
import org.apache.camel.component.xslt.XsltEndpoint;
import org.apache.camel.quarkus.support.xalan.XalanTransformerFactory;

@Recorder
public class CamelXsltRecorder {

    public RuntimeValue<XsltComponent> createXsltComponent(CamelXsltConfig config,
            RuntimeValue<RuntimeUriResolver.Builder> uriResolverBuilder) {
        final RuntimeUriResolver uriResolver = uriResolverBuilder.getValue().build();
        final QuarkusTransformerFactoryConfigurationStrategy strategy = new QuarkusTransformerFactoryConfigurationStrategy(
                config.packageName, config.features, uriResolver);
        final XsltComponent component = new XsltComponent();
        component.setUriResolver(uriResolver);
        component.setTransformerFactoryConfigurationStrategy(strategy);
        component.setTransformerFactoryClass(XalanTransformerFactory.class.getName());
        return new RuntimeValue<>(component);
    }

    public RuntimeValue<RuntimeUriResolver.Builder> createRuntimeUriResolverBuilder() {
        return new RuntimeValue<>(new RuntimeUriResolver.Builder());
    }

    public void addRuntimeUriResolverEntry(RuntimeValue<RuntimeUriResolver.Builder> builder, String templateUri,
            String transletClassName) {
        builder.getValue().entry(templateUri, transletClassName);
    }

    static class QuarkusTransformerFactoryConfigurationStrategy implements TransformerFactoryConfigurationStrategy {

        private final String packageName;
        private final RuntimeUriResolver uriResolver;
        private final Map<String, Boolean> features;

        public QuarkusTransformerFactoryConfigurationStrategy(String packageName, Map<String, Boolean> features,
                RuntimeUriResolver uriResolver) {
            this.uriResolver = uriResolver;
            this.packageName = packageName;
            this.features = features;
        }

        @Override
        public void configure(TransformerFactory tf, XsltEndpoint endpoint) {
            final String className = uriResolver.getTransletClassName(endpoint.getResourceUri());

            for (Map.Entry<String, Boolean> entry : features.entrySet()) {
                try {
                    tf.setFeature(entry.getKey(), entry.getValue());
                } catch (TransformerException e) {
                    throw new RuntimeException("Could not set TransformerFactory feature '"
                            + entry.getKey() + "' = " + entry.getValue(), e);
                }
            }

            tf.setAttribute("use-classpath", true);
            tf.setAttribute("translet-name", className);
            tf.setAttribute("package-name", packageName);
            tf.setErrorListener(new CamelXsltErrorListener());

            endpoint.setTransformerFactory(tf);
        }

    }
}
