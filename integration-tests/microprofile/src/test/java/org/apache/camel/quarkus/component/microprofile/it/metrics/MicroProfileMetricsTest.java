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
package org.apache.camel.quarkus.component.microprofile.it.metrics;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import org.apache.camel.ServiceStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class MicroProfileMetricsTest {

    private static final String CAMEL_CONTEXT_METRIC_TAG = ";camelContext=quarkus-camel-example";

    //@Test
    public void testMicroProfileMetricsCounter() {
        RestAssured.get("/microprofile-metrics/counter")
                .then()
                .statusCode(200);
        assertEquals(1, getMetricIntValue("camel-quarkus-counter"));
    }

    //@Test
    public void testMicroProfileMetricsConcurrentGauge() {
        for (int i = 0; i < 10; i++) {
            RestAssured.get("/microprofile-metrics/gauge/concurrent/increment")
                    .then()
                    .statusCode(200);
        }
        assertEquals(10, getMetricIntValue("camel-quarkus-concurrent-gauge.current"));

        for (int i = 0; i < 3; i++) {
            RestAssured.get("/microprofile-metrics/gauge/concurrent/decrement")
                    .then()
                    .statusCode(200);
        }
        assertEquals(7, getMetricIntValue("camel-quarkus-concurrent-gauge.current"));
    }

    //@Test
    public void testMicroProfileMetricsGauge() {
        RestAssured.get("/microprofile-metrics/gauge?value=10")
                .then()
                .statusCode(200);
        assertEquals(10, getMetricIntValue("camel-quarkus-gauge"));
    }

    //@Test
    public void testMicroProfileMetricsHistogram() {
        RestAssured.get("/microprofile-metrics/histogram?value=10")
                .then()
                .statusCode(200);
        assertEquals(10, getMetricIntValue("camel-quarkus-histogram.max"));
    }

    //@Test
    public void testMicroProfileMetricsMeter() {
        RestAssured.get("/microprofile-metrics/meter?mark=10")
                .then()
                .statusCode(200);
        assertEquals(10, getMetricIntValue("camel-quarkus-meter.count"));
    }

    //@Test
    public void testMicroProfileMetricsTimer() {
        RestAssured.get("/microprofile-metrics/timer")
                .then()
                .statusCode(200);
        assertTrue(getMetricFloatValue("camel-quarkus-timer.max") > 1.0);
    }

    //@Test
    public void testMicroProfileMetricsRoutePolicyFactory() {
        RestAssured.get("/microprofile-metrics/timer")
                .then()
                .statusCode(200);
        assertTrue(getMetricIntValue("camel.route.exchanges.total", CAMEL_CONTEXT_METRIC_TAG, "routeId=mp-metrics-timer") > 0);
    }

    //@Test
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

    //@Test
    public void testMicroProfileMetricsRouteEventNotifier() {
        assertTrue(getMetricIntValue("camel.route.count") >= 7);
        assertTrue(getMetricIntValue("camel.route.running.count") >= 7);
    }

    //@Test
    public void testMicroProfileMetricsExchangeEventNotifier() {
        RestAssured.get("/microprofile-metrics/log")
                .then()
                .statusCode(200);
        assertTrue(getMetricIntValue("camel.context.exchanges.total") > 0);
    }

    //@Test
    public void testMicroProfileMetricsCamelContextEventNotifier() {
        assertEquals(ServiceStatus.Started.ordinal(), getMetricIntValue("camel.context.status"));
        assertTrue(getMetricIntValue("camel.context.uptime") > 0);
    }

    //@Test
    public void testAdviceWith() {
        RestAssured.get("/microprofile-metrics/advicewith")
                .then()
                .statusCode(200);
        assertTrue(getMetricIntValue("camel.route.count") >= 7);
        assertTrue(getMetricIntValue("camel.route.running.count") >= 7);
    }

    //@Test
    public void testCountedProcessor() {
        for (int i = 0; i < 5; i++) {
            RestAssured.get("/microprofile-metrics/processor")
                    .then()
                    .statusCode(200);
        }

        int result = getApplicationMetrics().getInt("'" + CountedProcessor.class.getName() + ".custom.processor.count'");
        assertEquals(5, result);
    }

    private int getMetricIntValue(String metricName, String... tags) {
        return getApplicationMetrics().getInt(sanitizeMetricName(metricName, tags));
    }

    private float getMetricFloatValue(String metricName, String... tags) {
        return getApplicationMetrics().getFloat(sanitizeMetricName(metricName, tags));
    }

    private String sanitizeMetricName(String metricName, String... tags) {
        if (tags.length == 0) {
            tags = new String[] { CAMEL_CONTEXT_METRIC_TAG };
        }

        if (metricName.contains(".") && metricName.split("\\.").length > 2) {
            return String.format("'%s%s'", metricName, String.join(";", tags));
        }
        return metricName + String.join(";", tags);
    }

    private JsonPath getApplicationMetrics() {
        return RestAssured.given()
                .accept("application/json")
                .get("/q/metrics/application")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath();
    }
}
