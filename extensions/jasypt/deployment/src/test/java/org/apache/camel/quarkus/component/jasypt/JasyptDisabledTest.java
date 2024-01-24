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
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JasyptDisabledTest {
    private static final String SECURE_CONFIG_VALUE = "ENC(SomeSecureContent)";

    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.camel.jasypt.enabled", "false")
            .overrideConfigKey("secure.config.property", "ENC(SomeSecureContent)")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClass(JasyptRoutes.class));

    @Inject
    ProducerTemplate producerTemplate;

    @Test
    void propertyNotDecryptedIfJasyptDisabled() {
        String result = producerTemplate.requestBody("direct:start", null, String.class);
        assertEquals("Secure config value: " + SECURE_CONFIG_VALUE, result);
    }

    public static final class JasyptRoutes extends RouteBuilder {
        @Override
        public void configure() {
            from("direct:start")
                    .setBody().simple("Secure config value: ${properties:secure.config.property}");
        }
    }
}
