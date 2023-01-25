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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.dsl.xml.io.XmlRoutesBuilderLoader;
import org.apache.camel.quarkus.core.DisabledXMLRoutesDefinitionLoader;
import org.apache.camel.xml.jaxb.DefaultModelJAXBContextFactory;
import org.apache.camel.xml.jaxb.JaxbModelToXMLDumper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@QuarkusTest
public class CoreMainXmlJaxbTest {

    @Test
    public void testMainInstanceWithXmlRoutes() {
        JsonPath p = RestAssured.given()
                .accept(MediaType.APPLICATION_JSON)
                .get("/test/main/describe")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath();

        assertThat(p.getString("xml-model-dumper")).isEqualTo(JaxbModelToXMLDumper.class.getName());
        assertThat(p.getString("xml-model-factory")).isEqualTo(DefaultModelJAXBContextFactory.class.getName());

        assertThat(p.getString("xml-routes-definitions-loader"))
                .isEqualTo(DisabledXMLRoutesDefinitionLoader.class.getName());
        assertThat(p.getString("xml-routes-builder-loader"))
                .isEqualTo(XmlRoutesBuilderLoader.class.getName());

        assertThat(p.getList("routeBuilders", String.class))
                .isEmpty();

        List<String> routes = p.getList("routes", String.class);
        assertThat(routes)
                .contains("my-xml-route");
        assertThat(routes)
                .contains("templated-route");
        assertThat(routes)
                .contains("rest-route");
    }

    @Test
    public void testDumpRoutes() {
        await().atMost(10L, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS).until(() -> {
            String log = new String(Files.readAllBytes(Paths.get("target/quarkus.log")), StandardCharsets.UTF_8);
            return logContainsDumpedRoutes(log);
        });
    }

    private boolean logContainsDumpedRoutes(String log) {
        return log.contains("<route customId=\"true\" id=\"my-xml-route\">") &&
                log.contains("<route customId=\"true\" id=\"rest-route\">") &&
                log.contains("<rest customId=\"true\" id=\"greet\" path=\"/greeting\">") &&
                log.contains("<routeTemplate customId=\"true\" id=\"myTemplate\">");
    }
}
