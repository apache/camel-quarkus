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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class CamelMainRoutesIncludePatternWithDefaultLocationsDevModeTest {
    @RegisterExtension
    static final QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(CamelSupportResource.class)
                    .addAsResource(routeXml(), "camel/routes.xml")
                    .addAsResource(restsXml(), "camel-rest/rests.xml")
                    .addAsResource(routeTemplateXml(), "camel-template/templates.xml")
                    .addAsResource(applicationProperties(), "application.properties"));

    public static Asset routeXml() {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                    <routes>
                        <route id="route1-from-default-location">
                            <from uri="direct:greeting"/>
                            <setBody><constant>Hello World</constant></setBody>
                        </route>
                    </routes>""";
        return new StringAsset(xml);
    }

    public static Asset restsXml() {
        String xml = """
                <rests xmlns="http://camel.apache.org/schema/spring">
                    <rest id="rest1-from-default-location" path="/greeting">
                        <get path="/hello">
                            <to uri="direct:greeting"/>
                        </get>
                    </rest>
                </rests>""";
        return new StringAsset(xml);
    }

    public static Asset routeTemplateXml() {
        String xml = """
                <routeTemplates>
                    <routeTemplate id="template1-from-default-location">
                        <templateParameter name="name"/>
                        <templateParameter name="greeting"/>
                        <templateParameter name="myPeriod" defaultValue="3s"/>
                        <route>
                            <from uri="timer:{{name}}?period={{myPeriod}}"/>
                            <setBody><simple>{{greeting}} ${body}</simple></setBody>
                            <log message="${body}"/>
                        </route>
                    </routeTemplate>
                </routeTemplates>""";
        return new StringAsset(xml);
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
    public void testRoutesDiscoveryFromDefaultLocation() {
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Response res = RestAssured.when().get("/test/describe").thenReturn();

            assertThat(res.statusCode()).isEqualTo(200);
            assertThat(res.body().jsonPath().getList("routes", String.class)).containsOnly("route1-from-default-location");
            assertThat(res.body().jsonPath().getList("rests", String.class)).containsOnly("rest1-from-default-location");
            assertThat(res.body().jsonPath().getList("templates", String.class))
                    .containsOnly("template1-from-default-location");
        });

        TEST.modifyResourceFile("camel/routes.xml", xml -> xml.replaceAll("route1", "route2"));
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Response res = RestAssured.when().get("/test/describe").thenReturn();

            assertThat(res.statusCode()).isEqualTo(200);
            assertThat(res.body().jsonPath().getList("routes", String.class)).containsOnly("route2-from-default-location");
        });

        TEST.modifyResourceFile("camel-rest/rests.xml", xml -> xml.replaceAll("rest1", "rest2"));
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Response res = RestAssured.when().get("/test/describe").thenReturn();

            assertThat(res.statusCode()).isEqualTo(200);
            assertThat(res.body().jsonPath().getList("rests", String.class)).containsOnly("rest2-from-default-location");
        });

        TEST.modifyResourceFile("camel-template/templates.xml", xml -> xml.replaceAll("template1", "template2"));
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Response res = RestAssured.when().get("/test/describe").thenReturn();

            assertThat(res.statusCode()).isEqualTo(200);
            assertThat(res.body().jsonPath().getList("templates", String.class))
                    .containsOnly("template2-from-default-location");
        });
    }
}
