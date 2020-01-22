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
package org.apache.camel.quarkus.core;

import java.net.HttpURLConnection;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.apache.camel.quarkus.core.runtime.support.SupportListener;
import org.apache.camel.reactive.vertx.VertXReactiveExecutor;
import org.junit.jupiter.api.Test;

import static org.apache.camel.quarkus.core.CamelTestConditions.doesNotStartWith;
import static org.apache.camel.quarkus.core.CamelTestConditions.entry;
import static org.apache.camel.quarkus.core.CamelTestConditions.startsWith;
import static org.apache.camel.util.CollectionHelper.mapOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@QuarkusTest
public class CamelTest {
    @Test
    public void testProperties() {
        RestAssured.when().get("/test/property/camel.context.name").then().body(is("quarkus-camel-example"));
        RestAssured.when().get("/test/property/camel.component.timer.basic-property-binding").then().body(is("true"));
        RestAssured.when().get("/test/property/the.message").then().body(is("test"));
    }

    @Test
    public void timerPropertyPropagated() {
        RestAssured.when().get("/test/timer/property-binding").then().body(is("true"));
    }

    @Test
    public void testSetCamelContextName() {
        Response response = RestAssured.get("/test/context/name").andReturn();

        assertEquals(HttpURLConnection.HTTP_OK, response.getStatusCode());
        assertNotEquals("my-ctx-name", response.body().asString());

        RestAssured.given()
                .contentType(ContentType.TEXT).body("my-ctx-name")
                .post("/test/context/name")
                .then().body(is("my-ctx-name"));
    }

    @Test
    public void testMainInstance() {
        JsonPath p = RestAssured.given()
                .accept(MediaType.APPLICATION_JSON)
                .get("/test/main/describe")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath();

        assertThat(p.getString("routes-collector.type")).isEqualTo(CamelRoutesCollector.class.getName());
        assertThat(p.getString("routes-collector.type-registry")).isEqualTo(RegistryRoutesLoaders.Default.class.getName());
        assertThat(p.getString("routes-collector.type-xml")).isEqualTo(DisabledXmlRoutesLoader.class.getName());

        assertThat(p.getList("listeners", String.class))
                .containsOnly(CamelMainEventDispatcher.class.getName(), SupportListener.class.getName());
        assertThat(p.getList("routeBuilders", String.class))
                .contains(CamelRoute.class.getName())
                .doesNotContain(CamelRouteFiltered.class.getName());
        assertThat(p.getList("routes", String.class))
                .contains("keep-alive", "configure", "beforeStart", "produced", "endpointdsl")
                .doesNotContain("filtered");

        assertThat(p.getBoolean("autoConfigurationLogSummary")).isFalse();

        assertThat(p.getMap("registry.components", String.class, String.class)).isNotEmpty();
        assertThat(p.getMap("registry.dataformats", String.class, String.class)).isEmpty();
        assertThat(p.getMap("registry.languages", String.class, String.class)).containsExactlyInAnyOrderEntriesOf(mapOf(
                "header", "org.apache.camel.language.header.HeaderLanguage",
                "ref", "org.apache.camel.language.ref.RefLanguage",
                "simple", "org.apache.camel.language.simple.SimpleLanguage",
                "file", "org.apache.camel.language.simple.FileLanguage"));

        Map<String, String> factoryFinderMap = p.getMap("factory-finder.class-map", String.class, String.class);

        // dataformats
        assertThat(factoryFinderMap)
                .hasKeySatisfying(startsWith("META-INF/services/org/apache/camel/dataformat/"))
                .hasEntrySatisfying(entry(
                        "META-INF/services/org/apache/camel/dataformat/my-dataformat",
                        "org.apache.camel.quarkus.core.runtime.support.MyDataFormat"));

        // languages
        assertThat(factoryFinderMap)
                .hasKeySatisfying(startsWith("META-INF/services/org/apache/camel/language/"))
                .hasKeySatisfying(doesNotStartWith("META-INF/services/org/apache/camel/language/simple"))
                .hasKeySatisfying(doesNotStartWith("META-INF/services/org/apache/camel/language/file"))
                .hasKeySatisfying(doesNotStartWith("META-INF/services/org/apache/camel/language/ref"))
                .hasKeySatisfying(doesNotStartWith("META-INF/services/org/apache/camel/language/header"));

        // components
        assertThat(factoryFinderMap)
                .hasKeySatisfying(doesNotStartWith("META-INF/services/org/apache/camel/component/"));

        // misc
        assertThat(factoryFinderMap)
                .hasKeySatisfying(startsWith("META-INF/services/org/apache/camel/configurer/"));
    }

    @Test
    public void testReactiveExecutor() {
        JsonPath executor = RestAssured.when().get("/test/context/reactive-executor")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath();

        assertThat(executor.getString("class")).isEqualTo(VertXReactiveExecutor.class.getName());
        assertThat(executor.getBoolean("configured")).isTrue();
    }

    @Test
    public void testCustomTypeConverter() {
        RestAssured.given()
                .contentType(ContentType.TEXT).body("a:b")
                .accept(MediaType.APPLICATION_JSON)
                .post("/test/converter/my-pair")
                .then().body(
                        "key", is("a"),
                        "val", is("b"));
    }

    @Test
    public void testCustomComponent() {
        RestAssured.given()
                .accept(MediaType.APPLICATION_JSON)
                .get("/test/registry/component/direct")
                .then()
                .statusCode(200)
                .body(
                        "timeout", is("1234"),
                        "registry", is("repository"),
                        "registry-type", is("org.apache.camel.quarkus.core.RuntimeBeanRepository"));
    }
}
