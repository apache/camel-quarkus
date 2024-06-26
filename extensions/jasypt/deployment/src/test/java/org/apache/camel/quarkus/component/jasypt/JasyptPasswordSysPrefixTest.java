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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class JasyptPasswordSysPrefixTest {
    private static final String PASSWORD_PROPERTY_NAME = "jasyptDecryptSecret";

    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.camel.jasypt.password", "sys:" + PASSWORD_PROPERTY_NAME)
            .overrideConfigKey("secure.config", "ENC(GKJfy64eBDzxUuQCfArd6OjnAaW/oM9e)")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClass(JasyptRoutes.class));

    @Inject
    ProducerTemplate producerTemplate;

    @Test
    void passwordResolvedFromSystemProperty() {
        String result = producerTemplate.requestBody("direct:start", null, String.class);
        Assertions.assertEquals("Hello World", result);
    }

    public static final class JasyptRoutes extends RouteBuilder {
        @Override
        public void configure() throws Exception {
            from("direct:start")
                    .setBody().simple("${properties:secure.config}");
        }
    }
}
