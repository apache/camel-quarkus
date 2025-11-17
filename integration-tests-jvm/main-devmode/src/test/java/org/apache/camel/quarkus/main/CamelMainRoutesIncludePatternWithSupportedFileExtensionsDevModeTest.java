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
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class CamelMainRoutesIncludePatternWithSupportedFileExtensionsDevModeTest {
    static final String ROUTE_ID_BASE = "route";
    static final String XML_ROUTE_ID = "xml-" + ROUTE_ID_BASE;
    static final String CAMEL_XML_ROUTE_ID = "camel-" + XML_ROUTE_ID;
    static final String YAML_ROUTE_ID = "yaml-" + ROUTE_ID_BASE;
    static final String CAMEL_YAML_ROUTE_ID = "camel-" + YAML_ROUTE_ID;

    @RegisterExtension
    static final QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(CamelSupportResource.class)
                    .addAsResource(routeXml(XML_ROUTE_ID), "camel/routes.xml")
                    .addAsResource(routeXml(CAMEL_XML_ROUTE_ID), "camel/routes.camel.xml")
                    .addAsResource(routeYaml(YAML_ROUTE_ID), "camel/routes.yaml")
                    .addAsResource(routeYaml(CAMEL_YAML_ROUTE_ID), "camel/routes.camel.yaml")
                    .addAsResource(applicationProperties(), "application.properties"));

    public static Asset routeXml(String routeId) {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                    <routes>
                        <route id="%s">
                            <from uri="direct:%s"/>
                            <setBody><constant>Hello World</constant></setBody>
                        </route>
                    </routes>""".formatted(routeId, routeId);
        return new StringAsset(xml);
    }

    public static Asset routeYaml(String routeId) {
        String yaml = """
                    - route:
                        id: "%s"
                        from:
                          uri: "direct:%s"
                          steps:
                            - setBody:
                                constant: "Hello World"
                """.formatted(routeId, routeId);
        return new StringAsset(yaml);
    }

    public static Asset applicationProperties() {
        Writer writer = new StringWriter();

        Properties props = new Properties();
        props.setProperty("quarkus.banner.enabled", "false");

        try {
            props.store(writer, "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new StringAsset(writer.toString());
    }

    @Test
    void routesDiscoveryWithSupportedFileExtensions() {
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Response res = RestAssured.when().get("/test/describe").thenReturn();

            assertThat(res.statusCode()).isEqualTo(200);
            assertThat(res.body().jsonPath().getList("routes", String.class)).containsOnly(XML_ROUTE_ID, CAMEL_XML_ROUTE_ID,
                    YAML_ROUTE_ID, CAMEL_YAML_ROUTE_ID);
        });

        Map<String, String> fileToRouteIdMappings = Map.of("camel/routes.xml", XML_ROUTE_ID,
                "camel/routes.camel.xml", CAMEL_XML_ROUTE_ID,
                "camel/routes.yaml", YAML_ROUTE_ID,
                "camel/routes.camel.yaml", CAMEL_YAML_ROUTE_ID);

        fileToRouteIdMappings.forEach((path, routeId) -> {
            TEST.modifyResourceFile(path, route -> route.replaceAll(routeId, routeId + "-updated"));

            await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
                Response res = RestAssured.when().get("/test/describe").thenReturn();

                assertThat(res.statusCode()).isEqualTo(200);
                assertThat(res.body().jsonPath().getList("routes", String.class)).contains(routeId + "-updated");
            });
        });
    }
}
