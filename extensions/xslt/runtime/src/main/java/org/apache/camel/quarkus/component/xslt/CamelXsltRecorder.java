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
import javax.xml.transform.TransformerFactory;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.Endpoint;
import org.apache.camel.component.xslt.XsltComponent;
import org.apache.camel.component.xslt.XsltEndpoint;
import org.apache.camel.quarkus.support.xalan.XalanSupport;

@Recorder
public class CamelXsltRecorder {
    public RuntimeValue<XsltComponent> createXsltComponent(CamelXsltConfig config,
            RuntimeValue<RuntimeUriResolver.Builder> uriResolverBuilder) {
        return new RuntimeValue<>(new QuarkusXsltComponent(config, uriResolverBuilder.getValue().build()));
    }

    public RuntimeValue<RuntimeUriResolver.Builder> createRuntimeUriResolverBuilder() {
        return new RuntimeValue<>(new RuntimeUriResolver.Builder());
    }

    public void addRuntimeUriResolverEntry(RuntimeValue<RuntimeUriResolver.Builder> builder, String templateUri,
            String transletClassName) {
        builder.getValue().entry(templateUri, transletClassName);
    }

    static class QuarkusXsltComponent extends XsltComponent {
        private final CamelXsltConfig config;
        private final RuntimeUriResolver uriResolver;

        public QuarkusXsltComponent(CamelXsltConfig config, RuntimeUriResolver uriResolver) {
            this.config = config;
            this.uriResolver = uriResolver;
        }

        @Override
        protected void configureEndpoint(
                Endpoint endpoint,
                String remaining,
                Map<String, Object> parameters) throws Exception {

            final XsltEndpoint xsltEndpoint = (XsltEndpoint) endpoint;
            final String className = uriResolver.getTransletClassName(remaining);

            TransformerFactory tf = XalanSupport.newTransformerFactoryInstance();
            tf.setAttribute("use-classpath", true);
            tf.setAttribute("translet-name", className);
            tf.setAttribute("package-name", this.config.packageName);
            tf.setErrorListener(new CamelXsltErrorListener());

            xsltEndpoint.setTransformerFactory(tf);

            super.configureEndpoint(endpoint, remaining, parameters);

            xsltEndpoint.setUriResolver(uriResolver);
        }
    }
}
