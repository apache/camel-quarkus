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
package org.apache.camel.quarkus.component.micrometer.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.ResponseBodyExtractionOptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class MicrometerTest {

    @Test
    public void testMicrometerMetricsCounter() {
        RestAssured.get("/micrometer/counter")
                .then()
                .statusCode(200);
        assertEquals(1, getMetricValue(Integer.class, "counter", "camel-quarkus-counter"));
    }

    @Test
    public void testMicrometerSummary() {
        RestAssured.get("/micrometer/summary?value=10")
                .then()
                .statusCode(200);
        assertEquals(10, getMetricValue(Integer.class, "summary", "camel-quarkus-summary"));
    }

    @Test
    public void testMicrometerTimer() {
        RestAssured.get("/micrometer/timer")
                .then()
                .statusCode(200);
        assertTrue(getMetricValue(Integer.class, "timer", "camel-quarkus-timer") >= 100);
    }

    @Test
    public void testMicrometerRoutePolicyFactory() {
        RestAssured.get("/micrometer/timer")
                .then()
                .statusCode(200);
        assertTrue(getMetricValue(Integer.class, "counter", "CamelExchangesSucceeded", "routeId=micrometer-metrics-timer") > 0);
        assertEquals(0, getMetricValue(Integer.class, "counter", "CamelExchangesFailed", "routeId=micrometer-metrics-timer"));
    }

    @Test
    public void testMicrometerMessageHistoryFactory() {
        RestAssured.get("/micrometer/log")
                .then()
                .statusCode(200);
        String tags = "nodeId=log1,routeId=log";
        assertTrue(getMetricValue(Double.class, "timer", "CamelMessageHistory", tags) > 0.0);
    }

    @Test
    public void testMicrometerRouteEventNotifier() {
        assertTrue(getMetricValue(Integer.class, "gauge", "CamelRoutesAdded") >= 4);
        assertTrue(getMetricValue(Integer.class, "gauge", "CamelRoutesRunning") >= 4);
    }

    @Test
    public void testMicrometerExchangeEventNotifier() {
        RestAssured.get("/micrometer/log")
                .then()
                .statusCode(200);
        String tags = "endpointName=direct://log,eventType=ExchangeSentEvent";
        assertTrue(getMetricValue(Double.class, "timer", "CamelExchangeEventNotifier", tags) >= 0.0);
    }

    @Test
    public void testAnnotations() {
        RestAssured.get("/micrometer/annotations/call/1")
                .then()
                .statusCode(200);
        RestAssured.get("/micrometer/annotations/call/1")
                .then()
                .statusCode(200);

        assertEquals(2, getMetricValue(Double.class, "counter", "TestMetric.counted1", ""));
        assertTrue(getMetricValue(Double.class, "timer", "TestMetric.timed1", "") >= 2000);
    }

    @Test
    public void testQuarkusMetricsApi() {
        RestAssured.get("/micrometer/annotations/call/2")
                .then()
                .statusCode(200);

        assertEquals("Metric does not exist", getMetricValue(String.class, "counter", "TestMetric_wrong.counted", "", 500));
        assertEquals(1, getMetricValue(Double.class, "counter", "TestMetric.counted2", ""));
    }

    private <T> T getMetricValue(Class<T> as, String type, String name) {
        return getMetricValue(as, type, name, null);
    }

    private <T> T getMetricValue(Class<T> as, String type, String name, String tags) {
        return getMetricValue(as, type, name, tags, 200);
    }

    private <T> T getMetricValue(Class<T> as, String type, String name, String tags, int statusCode) {
        ResponseBodyExtractionOptions resp = RestAssured.given()
                .queryParam("tags", tags)
                .when()
                .get("/micrometer/metric/" + type + "/" + name)
                .then()
                .statusCode(statusCode)
                .extract()
                .body();

        if (as.equals(String.class)) {
            return (T) resp.asString();
        }

        return resp.as(as);
    }

    /**
     * Debug available metrics
     */
    private String dumpMetrics() {
        return RestAssured.get("/metrics")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();
    }
}
