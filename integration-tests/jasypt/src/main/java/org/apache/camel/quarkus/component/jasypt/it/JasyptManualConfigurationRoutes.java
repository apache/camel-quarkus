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
package org.apache.camel.quarkus.component.jasypt.it;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jasypt.JasyptPropertiesParser;
import org.apache.camel.component.properties.PropertiesComponent;
import org.eclipse.microprofile.config.inject.ConfigProperty;

public class JasyptManualConfigurationRoutes extends RouteBuilder {
    @ConfigProperty(name = "quarkus.camel.jasypt.enabled")
    boolean jasyptIntegrationEnabled;

    @Override
    public void configure() throws Exception {
        // Only enable when the automatic Jasypt config integration is disabled
        if (!jasyptIntegrationEnabled) {
            JasyptPropertiesParser jasypt = new JasyptPropertiesParser();
            jasypt.setPassword("2s3cr3t");

            PropertiesComponent component = (PropertiesComponent) getContext().getPropertiesComponent();
            jasypt.setPropertiesComponent(component);
            component.setPropertiesParser(jasypt);

            from("direct:decryptManualConfiguration")
                    .setBody().simple("{{greeting.secret}}");

            from("direct:decryptManualConfigurationExpression")
                    .setBody().simple("{{greeting.expression.secret}}");
        }
    }
}
