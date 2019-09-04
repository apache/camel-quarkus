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

        Map<String, Object> routeMetrics = getMetricMapValue("'org.apache.camel.route'");
        routeMetrics.forEach((k, v) -> {
            if (k.startsWith("count")) {
                assertTrue((Integer) v > 0);
            }
        });
    }

    @Test
    public void testMicroProfileMetricsMessageHistoryFactory() {
        Map<String, Object> messageHistoryMetrics = getMetricMapValue("'org.apache.camel.message.history'");
        messageHistoryMetrics.forEach((k, v) -> {
            if (k.startsWith("count")) {
                assertTrue((Integer) v > 0);
            }
        });
    }

    @Test
    public void testMicroProfileMetricsRouteEventNotifier() {
        Map<String, Object> routeMetrics = getMetricMapValue("'org.apache.camel.route.total'");
        routeMetrics.forEach((k, v) -> {
            if (k.startsWith("current")) {
                assertEquals(6, v);
            }
        });

        Map<String, Object> runningRouteMetrics = getMetricMapValue("'org.apache.camel.route.running.total'");
        runningRouteMetrics.forEach((k, v) -> {
            if (k.startsWith("current")) {
                assertEquals(6, v);
            }
        });
    }

    @Test
    public void testMicroProfileMetricsExchangeEventNotifier() {
        Map<String, Object> exchangeMetrics = getMetricMapValue("'org.apache.camel.exchange'");
        exchangeMetrics.forEach((k, v) -> {
            if (k.startsWith("total")) {
                assertTrue((Integer) v > 0);
            }
        });
    }

    private int getMetricIntValue(String metricName) {
        return getApplicationMetrics().getInt(metricName + CAMEL_CONTEXT_METRIC_TAG);
    }

    private float getMetricFloatValue(String metricName) {
        return getApplicationMetrics().getFloat(metricName + CAMEL_CONTEXT_METRIC_TAG);
    }

    private Map<String, Object> getMetricMapValue(String metricName) {
        return getApplicationMetrics().getMap(metricName);
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
