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

import java.net.HttpURLConnection;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.apache.camel.quarkus.core.DisabledModelToXMLDumper;
import org.apache.camel.quarkus.core.DisabledXMLRoutesDefinitionLoader;
import org.apache.camel.quarkus.core.RegistryRoutesLoaders;
import org.apache.camel.quarkus.it.support.mainlistener.CustomMainListener;
import org.apache.camel.reactive.vertx.VertXReactiveExecutor;
import org.apache.camel.reactive.vertx.VertXThreadPoolFactory;
import org.apache.camel.support.DefaultLRUCacheFactory;
import org.junit.jupiter.api.Disabled;

import static org.apache.camel.quarkus.test.Conditions.doesNotStartWith;
import static org.apache.camel.quarkus.test.Conditions.entry;
import static org.apache.camel.quarkus.test.Conditions.startsWith;
import static org.apache.camel.util.CollectionHelper.mapOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@QuarkusTest
public class CoreMainTest {
    @Disabled
    //@Test
    public void testProperties() {
        RestAssured.when().get("/test/property/camel.context.name").then().body(is("quarkus-camel-example"));
        RestAssured.when().get("/test/property/the.message").then().body(is("test"));
    }

    //@Test
    public void testSetCamelContextName() {
        Response response = RestAssured.get("/test/context/name").andReturn();

        assertEquals(HttpURLConnection.HTTP_OK, response.getStatusCode());
        assertNotEquals("my-ctx-name", response.body().asString());

        RestAssured.given()
                .contentType(ContentType.TEXT).body("my-ctx-name")
                .post("/test/context/name")
                .then().body(is("my-ctx-name"));
    }

    //@Test
    public void testMainInstance() {
        JsonPath p = RestAssured.given()
                .accept(MediaType.APPLICATION_JSON)
                .get("/test/main/describe")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath();

        assertThat(p.getString("xml-loader")).isEqualTo(DisabledXMLRoutesDefinitionLoader.class.getName());
        assertThat(p.getString("xml-model-dumper")).isEqualTo(DisabledModelToXMLDumper.class.getName());

        assertThat(p.getString("routes-collector.type")).isEqualTo(CamelMainRoutesCollector.class.getName());
        assertThat(p.getString("routes-collector.type-registry")).isEqualTo(RegistryRoutesLoaders.Default.class.getName());
        assertThat(p.getString("routes-collector.type-xml")).isEqualTo(DisabledXMLRoutesDefinitionLoader.class.getName());

        assertThat(p.getList("listeners", String.class))
                .containsAnyOf(CamelMainEventBridge.class.getName(), CustomMainListener.class.getName());
        assertThat(p.getList("routeBuilders", String.class))
                .contains(CamelRoute.class.getName())
                .doesNotContain(CamelRouteFiltered.class.getName());
        assertThat(p.getList("routes", String.class))
                .contains("keep-alive", "configure", "beforeStart", "produced", "endpointdsl")
                .doesNotContain("filtered");

        assertThat(p.getString("lru-cache-factory")).isEqualTo(DefaultLRUCacheFactory.class.getName());
        assertThat(p.getBoolean("autoConfigurationLogSummary")).isFalse();

        assertThat(p.getMap("registry.components", String.class, String.class)).isNotEmpty();
        assertThat(p.getMap("registry.dataformats", String.class, String.class)).isEmpty();
        assertThat(p.getMap("registry.languages", String.class, String.class)).containsExactlyInAnyOrderEntriesOf(mapOf(
                "constant", "org.apache.camel.language.constant.ConstantLanguage",
                "file", "org.apache.camel.language.simple.FileLanguage",
                "header", "org.apache.camel.language.header.HeaderLanguage",
                "simple", "org.apache.camel.language.simple.SimpleLanguage",
                "ref", "org.apache.camel.language.ref.RefLanguage"));

        Map<String, String> factoryFinderMap = p.getMap("factory-finder.class-map", String.class, String.class);

        // dataformats
        assertThat(factoryFinderMap)
                .hasKeySatisfying(startsWith("META-INF/services/org/apache/camel/dataformat/"))
                .hasEntrySatisfying(entry(
                        "META-INF/services/org/apache/camel/dataformat/my-dataformat",
                        "org.apache.camel.quarkus.it.support.dataformat.MyDataformat"));

        // languages
        assertThat(factoryFinderMap)
                .hasKeySatisfying(startsWith("META-INF/services/org/apache/camel/language/"))
                .hasKeySatisfying(doesNotStartWith("META-INF/services/org/apache/camel/language/constant"))
                .hasKeySatisfying(doesNotStartWith("META-INF/services/org/apache/camel/language/file"))
                .hasKeySatisfying(doesNotStartWith("META-INF/services/org/apache/camel/language/header"))
                .hasKeySatisfying(doesNotStartWith("META-INF/services/org/apache/camel/language/ref"))
                .hasKeySatisfying(doesNotStartWith("META-INF/services/org/apache/camel/language/simple"));

        // components
        assertThat(factoryFinderMap)
                .hasKeySatisfying(doesNotStartWith("META-INF/services/org/apache/camel/component/"));

        // misc
        assertThat(factoryFinderMap)
                .hasKeySatisfying(startsWith("META-INF/services/org/apache/camel/configurer/"));

        // core
        assertThat(factoryFinderMap)
                .hasKeySatisfying(doesNotStartWith("META-INF/services/org/apache/camel/properties-component-factory"))
                .hasKeySatisfying(doesNotStartWith("META-INF/services/org/apache/camel/reactive-executor"));

        // rest properties are configured through CamelContext, this test is to spot changes
        // done to underlying camel-main implementation that would prevent to configure it
        // consistently across runtimes using camel-main properties.
        assertThat(p.getString("config.rest-port")).isEqualTo("9876");

        // resilience4j properties are configured through ModelCamelContext, this test is
        // to ensure that FastCamelContext won't change in a way it cannot be configured
        // consistently across runtimes using camel-main properties.
        assertThat(p.getString("config.resilience4j-sliding-window-size")).isEqualTo("1234");

        // ensure reflection is not used
        assertThat(p.getLong("bean-introspection-invocations")).isEqualTo(0);

    }

    //@Test
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

    //@Test
    public void testThreadPoolFactory() {
        JsonPath executor = RestAssured.when().get("/test/context/thread-pool-factory")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath();

        assertThat(executor.getString("class")).isEqualTo(VertXThreadPoolFactory.class.getName());
        assertThat(executor.getBoolean("configured")).isTrue();
    }

    //@Test
    public void testCustomTypeConverter() {
        RestAssured.given()
                .contentType(ContentType.TEXT).body("a:b")
                .accept(MediaType.APPLICATION_JSON)
                .post("/test/converter/my-pair")
                .then().body(
                        "key", is("a"),
                        "val", is("b"));
    }

    //@Test
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

    //@Test
    public void testGetStringFromRegistry() {
        RestAssured.given()
                .accept(MediaType.TEXT_PLAIN)
                .get("/test/registry/string/stringFromRegistry")
                .then()
                .statusCode(200)
                .body(is("String From Registry"));
    }
}
