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
package org.apache.camel.quarkus.component.microprofile.metrics.it;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class MicroProfileMetricsTest {

    private static final String CAMEL_CONTEXT_METRIC_TAG = ";camelContext=quarkus-camel-example";

    @Test
    public void testMicroProfileMetricsCounter() {
        RestAssured.get("/microprofile-metrics/counter")
            .then()
            .statusCode(200);
        assertEquals(1, getMetricIntValue("camel-quarkus-counter"));
    }

    @Test
    public void testMicroProfileMetricsGauge() {
        for (int i = 0; i < 10; i++) {
            RestAssured.get("/microprofile-metrics/gauge/increment")
                .then()
                .statusCode(200);
        }
        assertEquals(10, getMetricIntValue("camel-quarkus-gauge.current"));

        for (int i = 0; i < 3; i++) {
            RestAssured.get("/microprofile-metrics/gauge/decrement")
                .then()
                .statusCode(200);
        }
        assertEquals(7, getMetricIntValue("camel-quarkus-gauge.current"));
    }

    @Test
    public void testMicroProfileMetricsHistogram() {
        RestAssured.get("/microprofile-metrics/histogram?value=10")
            .then()
            .statusCode(200);
        assertEquals(10, getMetricIntValue("camel-quarkus-histogram.max"));
    }

    @Test
    public void testMicroProfileMetricsMeter() {
        RestAssured.get("/microprofile-metrics/meter?mark=10")
            .then()
            .statusCode(200);
        assertEquals(10, getMetricIntValue("camel-quarkus-meter.count"));
    }

    @Test
    public void testMicroProfileMetricsTimer() {
        RestAssured.get("/microprofile-metrics/timer")
            .then()
            .statusCode(200);
        assertTrue(getMetricFloatValue("camel-quarkus-timer.max") > 1.0);
    }

    @Test
    public void testMicroProfileMetricsRoutePolicyFactory() {
        RestAssured.get("/microprofile-metrics/timer")
            .then()
            .statusCode(200);
        assertTrue(getMetricIntValue("camel.route.exchanges.total", CAMEL_CONTEXT_METRIC_TAG, "routeId=route6") > 0);
    }

    @Test
    public void testMicroProfileMetricsMessageHistoryFactory() {
        RestAssured.get("/microprofile-metrics/log")
            .then()
            .statusCode(200);

        Map<String, Object> exchangeMetrics = getApplicationMetrics().getMap("'camel.message.history.processing'");
        exchangeMetrics.forEach((k, v) -> {
            if (k.startsWith("total")) {
                assertTrue((Integer) v > 0);
            }
        });
    }

    @Test
    public void testMicroProfileMetricsRouteEventNotifier() throws InterruptedException {
        assertEquals(6, getMetricIntValue("camel.route.count"));
        assertEquals(6, getMetricIntValue("camel.route.running.count"));
    }

    @Test
    public void testMicroProfileMetricsExchangeEventNotifier() {
        RestAssured.get("/microprofile-metrics/log")
            .then()
            .statusCode(200);
        assertTrue(getMetricIntValue("camel.context.exchanges.total") > 0);
    }

    private int getMetricIntValue(String metricName, String... tags) {
        return getApplicationMetrics().getInt(sanitizeMetricName(metricName, tags));
    }

    private float getMetricFloatValue(String metricName, String... tags) {
        return getApplicationMetrics().getFloat(sanitizeMetricName(metricName, tags));
    }

    private Map<String, Object> getMetricMapValue(String metricName, String... tags) {
        return getApplicationMetrics().getMap(sanitizeMetricName(metricName, tags));
    }

    private String sanitizeMetricName(String metricName, String... tags) {
        if (tags.length == 0) {
            tags = new String[] {CAMEL_CONTEXT_METRIC_TAG};
        }

        if (metricName.contains(".") && metricName.split("\\.").length > 2) {
            return String.format("'%s%s'", metricName, String.join(";", tags));
        }
        return metricName + String.join(";", tags);
    }

    private JsonPath getApplicationMetrics() {
        return RestAssured.given()
            .accept("application/json")
            .get("/metrics/application")
            .then()
            .statusCode(200)
            .extract()
            .body()
            .jsonPath();
    }
}
