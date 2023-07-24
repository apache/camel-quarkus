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
package org.apache.camel.quarkus.k.loader;

import java.io.IOException;
import java.io.InputStream;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.quarkus.k.listener.ContextConfigurer;
import org.apache.camel.quarkus.k.listener.SourcesConfigurer;
import org.apache.camel.util.IOHelper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class LoaderTest {

    @Test
    public void testServices() {
        JsonPath p = RestAssured.given()
                .accept(MediaType.APPLICATION_JSON)
                .get("/test/services")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath();

        assertThat(p.getList("services", String.class)).contains(
                ContextConfigurer.class.getName(),
                SourcesConfigurer.class.getName());
    }

    @Test
    public void testLoadGroovyRoutes() throws IOException {
        String code;
        try (InputStream is = LoaderTest.class.getResourceAsStream("/routes.groovy")) {
            code = IOHelper.loadText(is);
        }

        JsonPath p = RestAssured.given()
                .contentType(MediaType.TEXT_PLAIN)
                .accept(MediaType.APPLICATION_JSON)
                .body(code)
                .post("/test/load-routes/groovy/MyRoute")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath();

        assertThat(p.getList("components", String.class)).contains("direct", "log");
        assertThat(p.getList("routes", String.class)).contains("groovy");
        assertThat(p.getList("endpoints", String.class)).contains("direct://groovy", "log://groovy");
    }

    @Test
    public void testLoadJavaRoutes() throws IOException {
        String code;

        try (InputStream is = LoaderTest.class.getResourceAsStream("/MyRoutes.java")) {
            code = IOHelper.loadText(is);
        }

        JsonPath p = RestAssured.given()
                .contentType(MediaType.TEXT_PLAIN)
                .accept(MediaType.APPLICATION_JSON)
                .body(code)
                .post("/test/load-routes/java/MyRoutes")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath();

        assertThat(p.getList("components", String.class)).contains("direct", "log");
        assertThat(p.getList("routes", String.class)).contains("java");
        assertThat(p.getList("endpoints", String.class)).contains("direct://java", "log://java");
    }

    @Test
    public void testLoadJavascriptRoutes() throws IOException {
        String code;

        try (InputStream is = LoaderTest.class.getResourceAsStream("/routes.js")) {
            code = IOHelper.loadText(is);
        }

        JsonPath p = RestAssured.given()
                .contentType(MediaType.TEXT_PLAIN)
                .accept(MediaType.APPLICATION_JSON)
                .body(code)
                .post("/test/load-routes/js/MyRoute")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath();

        assertThat(p.getList("components", String.class)).contains("direct", "log");
        assertThat(p.getList("routes", String.class)).contains("js");
        assertThat(p.getList("endpoints", String.class)).contains("direct://js", "log://js");
    }

    @Test
    public void testLoadJshRoutes() throws IOException {
        String code;

        try (InputStream is = LoaderTest.class.getResourceAsStream("/routes.jsh")) {
            code = IOHelper.loadText(is);
        }

        JsonPath p = RestAssured.given()
                .contentType(MediaType.TEXT_PLAIN)
                .accept(MediaType.APPLICATION_JSON)
                .body(code)
                .post("/test/load-routes/jsh/routes")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath();

        assertThat(p.getList("components", String.class)).contains("direct", "log");
        assertThat(p.getList("routes", String.class)).contains("jsh");
        assertThat(p.getList("endpoints", String.class)).contains("direct://jsh", "log://jsh");
    }

    @Test
    public void testLoadKotlinRoutes() throws IOException {

        String code;

        try (InputStream is = LoaderTest.class.getResourceAsStream("/routes.kts")) {
            code = IOHelper.loadText(is);
        }

        JsonPath p = RestAssured.given()
                .contentType(MediaType.TEXT_PLAIN)
                .accept(MediaType.APPLICATION_JSON)
                .body(code)
                .post("/test/load-routes/kts/MyRoute")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath();

        assertThat(p.getList("components", String.class)).contains("direct", "log");
        assertThat(p.getList("routes", String.class)).contains("kotlin");
        assertThat(p.getList("endpoints", String.class)).contains("direct://kotlin", "log://kotlin");
    }

    @Test
    public void testLoadXmlRoutes() throws IOException {
        String code;

        try (InputStream is = LoaderTest.class.getResourceAsStream("/routes.xml")) {
            code = IOHelper.loadText(is);
        }

        JsonPath p = RestAssured.given()
                .contentType(MediaType.TEXT_PLAIN)
                .accept(MediaType.APPLICATION_JSON)
                .body(code)
                .post("/test/load-routes/xml/MyRoute")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath();

        assertThat(p.getList("components", String.class)).contains("direct", "log");
        assertThat(p.getList("routes", String.class)).contains("xml");
        assertThat(p.getList("endpoints", String.class)).contains("direct://xml", "log://xml");
    }

    @Test
    public void testLoadYamlRoutes() throws IOException {
        String code;

        try (InputStream is = LoaderTest.class.getResourceAsStream("/routes.yaml")) {
            code = IOHelper.loadText(is);
        }

        JsonPath p = RestAssured.given()
                .contentType(MediaType.TEXT_PLAIN)
                .accept(MediaType.APPLICATION_JSON)
                .body(code)
                .post("/test/load-routes/yaml/MyRoute")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath();

        assertThat(p.getList("components", String.class)).contains("direct", "log");
        assertThat(p.getList("routes", String.class)).contains("yaml");
        assertThat(p.getList("endpoints", String.class)).contains("direct://yaml", "log://yaml");
    }
}
