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
package org.apache.camel.quarkus.main;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class CamelMainRoutesIncludePatternWithNoPrefixDevModeTest {

    @RegisterExtension
    static final QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(CamelSupportResource.class)
                    .addAsResource(initialRoutesXml(), "routes/routes.xml")
                    .addAsResource(applicationProperties(), "application.properties"));

    public static Asset initialRoutesXml() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><routes xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                + "xmlns=\"http://camel.apache.org/schema/spring\" xsi:schemaLocation=\"http://camel.apache.org/schema/spring "
                + "http://camel.apache.org/schema/spring/camel-spring.xsd\"><route id=\"r1-no-prefix\">"
                + "<from uri=\"direct:start\"/>"
                + "<to uri=\"direct:end\"/></route></routes>";

        return new StringAsset(xml);
    }

    public static Asset applicationProperties() {
        Writer writer = new StringWriter();

        Properties props = new Properties();
        props.setProperty("quarkus.banner.enabled", "false");
        props.setProperty("camel.main.routes-include-pattern", "routes/routes.xml");

        try {
            props.store(writer, "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new StringAsset(writer.toString());
    }

    //@Test
    public void testRoutesDiscovery() {
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Response res = RestAssured.when().get("/test/describe").thenReturn();

            assertThat(res.statusCode()).isEqualTo(200);
            assertThat(res.body().jsonPath().getList("routes", String.class)).containsOnly("r1-no-prefix");
        });

        TEST.modifyResourceFile("routes/routes.xml", xml -> xml.replaceAll("r1", "r2"));

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Response res = RestAssured.when().get("/test/describe").thenReturn();

            assertThat(res.statusCode()).isEqualTo(200);
            assertThat(res.body().jsonPath().getList("routes", String.class)).containsOnly("r2-no-prefix");
        });
    }
}
