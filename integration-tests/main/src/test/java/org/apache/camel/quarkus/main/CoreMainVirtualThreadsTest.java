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
import java.util.Set;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.EnabledForJreRange;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.ServiceStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Integration tests for {@code camel.main.virtualThreadsEnabled} support (Camel 4.19.0+).
 * <p>
 * Virtual threads require JDK 21+ so these tests are gated with {@link EnabledForJreRange}.
 * Tests use a dedicated Quarkus profile ({@link VirtualThreadsTestProfile}) that sets
 * {@code camel.main.virtualThreadsEnabled=true} only for this test class, ensuring isolation
 * from other tests in the module.
 */
@QuarkusTest
@TestProfile(CoreMainVirtualThreadsTest.VirtualThreadsTestProfile.class)
@EnabledForJreRange(min = 21)
public class CoreMainVirtualThreadsTest {

    public static class VirtualThreadsTestProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("camel.main.virtualThreadsEnabled", "true");
        }

        @Override
        public String getConfigProfile() {
            return null;
        }

        @Override
        public Set<Class<?>> getEnabledAlternatives() {
            return Set.of();
        }

        @Override
        public String getArtifactsDestinationPath() {
            return null;
        }

        @Override
        public boolean disableGlobalTestResources() {
            return false;
        }
    }

    /**
     * Verifies the app starts successfully with virtual threads enabled.
     */
    @Test
    public void testStartupWithVirtualThreads() {
        RestAssured.when()
                .get("/test/runtime/status")
                .then()
                .statusCode(HttpURLConnection.HTTP_OK)
                .body(is(ServiceStatus.Started.name()));
    }

    /**
     * Verifies that Camel routes are present and the context is healthy with virtual threads.
     */
    @Test
    public void testRoutesPresentWithVirtualThreads() {
        var routes = RestAssured.given()
                .accept(MediaType.APPLICATION_JSON)
                .get("/test/main/describe")
                .then()
                .statusCode(HttpURLConnection.HTTP_OK)
                .extract()
                .jsonPath()
                .getList("routes");

        assertThat(routes).isNotNull().isNotEmpty();
    }

    /**
     * Verifies property placeholder resolution works with virtual threads.
     */
    @Test
    public void testPropertyResolutionWithVirtualThreads() {
        RestAssured.when()
                .get("/test/property/camel.context.name")
                .then()
                .statusCode(HttpURLConnection.HTTP_OK)
                .body(is("quarkus-camel-example"));

        RestAssured.when()
                .get("/test/property/the.message")
                .then()
                .statusCode(HttpURLConnection.HTTP_OK)
                .body(is("test"));
    }

    /**
     * Verifies that custom type conversion works with virtual threads.
     */
    @Test
    public void testTypeConversionWithVirtualThreads() {
        RestAssured.given()
                .contentType(ContentType.TEXT).body("key:value")
                .accept(MediaType.APPLICATION_JSON)
                .post("/test/converter/my-pair")
                .then()
                .statusCode(HttpURLConnection.HTTP_OK)
                .body("key", is("key"))
                .body("val", is("value"));
    }

    /**
     * Verifies that custom components resolve correctly with virtual threads.
     */
    @Test
    public void testCustomComponentWithVirtualThreads() {
        RestAssured.given()
                .accept(MediaType.APPLICATION_JSON)
                .get("/test/registry/component/direct")
                .then()
                .statusCode(HttpURLConnection.HTTP_OK)
                .body("timeout", is("1234"))
                .body("registry", is("repository"));
    }

    /**
     * Verifies the reactive executor is available with virtual threads.
     */
    @Test
    public void testReactiveExecutorWithVirtualThreads() {
        RestAssured.given()
                .accept(MediaType.APPLICATION_JSON)
                .get("/test/context/reactive-executor")
                .then()
                .statusCode(HttpURLConnection.HTTP_OK);
    }

    /**
     * Verifies the thread pool factory is available with virtual threads.
     */
    @Test
    public void testThreadPoolFactoryWithVirtualThreads() {
        RestAssured.given()
                .accept(MediaType.APPLICATION_JSON)
                .get("/test/context/thread-pool-factory")
                .then()
                .statusCode(HttpURLConnection.HTTP_OK);
    }

    /**
     * Verifies graceful shutdown with virtual threads.
     * The JMX connector service should start cleanly when VT are enabled.
     */
    @Test
    public void testGracefulShutdownWithVirtualThreads() {
        RestAssured.given()
                .accept(MediaType.TEXT_PLAIN)
                .get("/test/service/jmx-connector/status")
                .then()
                .statusCode(HttpURLConnection.HTTP_OK)
                .body(is(ServiceStatus.Started.name().toUpperCase()));

        RestAssured.given()
                .accept(MediaType.TEXT_PLAIN)
                .get("/test/service/jmx-connector/expected/connect/state")
                .then()
                .statusCode(HttpURLConnection.HTTP_OK)
                .body(is("true"));
    }
}
