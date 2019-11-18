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
import org.apache.camel.component.xslt.XsltComponent;
import org.apache.camel.component.xslt.XsltEndpoint;
import org.apache.camel.quarkus.support.xalan.XalanSupport;
import org.apache.camel.util.FileUtil;
import org.apache.camel.util.StringHelper;

@Recorder
public class CamelXsltRecorder {
    public RuntimeValue<XsltComponent> createXsltComponent(CamelXsltConfig config) {
        return new RuntimeValue<>(new QuarkusXsltComponent(config));
    }

    static class QuarkusXsltComponent extends XsltComponent {
        private final CamelXsltConfig config;

        public QuarkusXsltComponent(CamelXsltConfig config) {
            this.config = config;
        }

        @Override
        protected void configureEndpoint(
                XsltEndpoint endpoint,
                String remaining,
                Map<String, Object> parameters) throws Exception {

            final String fileName = FileUtil.stripExt(remaining, true);
            final String className = StringHelper.capitalize(fileName, true);

            TransformerFactory tf = XalanSupport.newTransformerFactoryInstance();
            tf.setAttribute("use-classpath", true);
            tf.setAttribute("translet-name", className);
            tf.setAttribute("package-name", this.config.packageName);
            tf.setErrorListener(new CamelXsltErrorListener());

            endpoint.setTransformerFactory(tf);

            super.configureEndpoint(endpoint, remaining, parameters);
        }
    }

}
