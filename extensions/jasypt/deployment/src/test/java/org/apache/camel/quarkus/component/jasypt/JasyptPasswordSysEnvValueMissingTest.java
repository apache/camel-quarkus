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
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jasypt.JasyptPropertiesParser;
import org.apache.camel.component.properties.PropertiesComponent;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class JasyptPasswordSysEnvValueMissingTest {
    private static final String PASSWORD_VAR_NAME = "JASYPT_BAD_DECRYPT_SECRET";

    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .overrideConfigKey("greeting.secret", "ENC(GKJfy64eBDzxUuQCfArd6OjnAaW/oM9e)")
            .overrideConfigKey("quarkus.camel.jasypt.password", "sysenv:" + PASSWORD_VAR_NAME)
            .setExpectedException(IllegalStateException.class)
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClass(JasyptRoutes.class));

    @Test
    void nonExistentPasswordEnvironmentVariableHandledGracefully() {
        // Nothing to test as we just verify the application fails to start
    }

    public static final class JasyptRoutes extends RouteBuilder {
        @Override
        public void configure() {
            JasyptPropertiesParser jasypt = new JasyptPropertiesParser();
            jasypt.setPassword("2s3cr3t");

            PropertiesComponent component = (PropertiesComponent) getContext().getPropertiesComponent();
            jasypt.setPropertiesComponent(component);
            component.setPropertiesParser(jasypt);

            from("direct:decryptManualConfiguration")
                    .setBody().simple("{{greeting.secret}}");
        }
    }
}
