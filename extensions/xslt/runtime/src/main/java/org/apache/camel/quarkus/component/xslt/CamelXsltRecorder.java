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
import javax.xml.transform.URIResolver;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.CamelContext;
import org.apache.camel.component.xslt.DefaultXsltUriResolverFactory;
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
        component.setUriResolverFactory(new QuarkusXsltUriResolverFactory(uriResolver));
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

    static class QuarkusXsltUriResolverFactory extends DefaultXsltUriResolverFactory {
        private final RuntimeUriResolver uriResolver;

        public QuarkusXsltUriResolverFactory(RuntimeUriResolver uriResolver) {
            this.uriResolver = uriResolver;
        }

        @Override
        public URIResolver createUriResolver(CamelContext camelContext, String resourceUri) {
            // It is supposed to catch cases where we compile the translet at build time which is for classpath: XSLT
            // resources in both JVM and native mode.
            // Otherwise, all other cases will be handled by the default XsltUriResolver which is able to load a resource
            // at runtime. So it is only supported in JVM mode.
            if (uriResolver.getTransletClassName(resourceUri) != null) {
                return uriResolver;
            } else {
                return super.createUriResolver(camelContext, resourceUri);
            }
        }
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
            if (className != null) {
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
}
