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
import io.restassured.path.json.JsonPath;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Test that verifies Camel Java route hot reload works correctly after the CamelHotReplacementSetup
 * fix for race conditions. This ensures the fix doesn't break legitimate route reloading while
 * preventing the race condition between Quarkus and Camel scans.
 *
 * See: https://github.com/apache/camel-quarkus/issues/8318
 */
class CamelRouteHotReloadDevModeTest {
    private static final String ORIGINAL_ROUTE_ID = "hot-reload-route";
    private static final String MODIFIED_ROUTE_ID = "hot-reload-route-modified";

    @RegisterExtension
    static final QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(CamelSupportResource.class)
                    .addClass(HotReloadTestRoute.class)
                    .addAsResource(applicationProperties(), "application.properties"));

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
    void testJavaRouteHotReload() {
        // Verify original route is loaded
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            JsonPath response = RestAssured.when().get("/test/describe").jsonPath();
            assertThat(response.getList("routes"))
                    .as("Original route should be loaded")
                    .contains(ORIGINAL_ROUTE_ID);
        });

        // Modify the Java route - change the route ID
        TEST.modifySourceFile("HotReloadTestRoute.java",
                s -> s.replace(
                        String.format("routeId(\"%s\")", ORIGINAL_ROUTE_ID),
                        String.format("routeId(\"%s\")", MODIFIED_ROUTE_ID)));

        // Verify the modified route is loaded and old route is removed
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            JsonPath response = RestAssured.when().get("/test/describe").jsonPath();
            assertThat(response.getList("routes"))
                    .as("Modified route should be loaded after hot reload")
                    .contains(MODIFIED_ROUTE_ID)
                    .doesNotContain(ORIGINAL_ROUTE_ID);
        });
    }
}
