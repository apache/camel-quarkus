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
package org.apache.camel.quarkus.component.jasypt;

import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jasypt.JasyptPropertiesParser;
import org.apache.camel.component.properties.PropertiesComponent;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Note: This scenario is tested here, instead of in the integration-tests module, since quarkus.camel.jasypt.enabled is
 * fixed at build time. Thus, it's not possible for the property to be overridden via QuarkusTestProfile.
 */
class JasyptManualConfigurationTest {

    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.camel.jasypt.enabled", "false")
            .overrideConfigKey("greeting.secret", "ENC(GKJfy64eBDzxUuQCfArd6OjnAaW/oM9e)")
            .overrideConfigKey("greeting.expression.secret", "${greeting.secret} From Expression")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClass(JasyptRoutes.class));

    @Inject
    ProducerTemplate producerTemplate;

    @Test
    void manualJasyptConfiguration() {
        String result = producerTemplate.requestBody("direct:decryptManualConfiguration", null, String.class);
        assertEquals("Hello World", result);
    }

    @Test
    void manualJasyptConfigurationExpression() {
        String result = producerTemplate.requestBody("direct:decryptManualConfigurationExpression", null, String.class);
        assertEquals("Hello World From Expression", result);
    }

    @Test
    void jasyptConfigInterceptorInactive() {
        // Verify the integration with SmallRye config is deactivated. E.g. the raw encrypted values are returned.
        Config config = ConfigProvider.getConfig();
        String result = config.getValue("greeting.expression.secret", String.class);
        assertTrue(result.startsWith("ENC("));
    }

    public static final class JasyptRoutes extends RouteBuilder {
        @Override
        public void configure() throws Exception {
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
