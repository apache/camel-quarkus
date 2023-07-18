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

import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.Set;

import javax.management.Attribute;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import io.quarkus.test.junit.DisabledOnIntegrationTest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class MicrometerTest extends AbstractMicrometerTest {

    @Test
    public void testMicrometerMetricsCounter() throws Exception {
        RestAssured.get("/micrometer/counter/0")
                .then()
                .statusCode(200);
        assertEquals(1, getMetricValue(Integer.class, "counter", "camel-quarkus-counter", "customTag=prometheus"));

        RestAssured.get("/micrometer/counter/5")
                .then()
                .statusCode(200);
        assertEquals(6, getMetricValue(Integer.class, "counter", "camel-quarkus-counter"));

        //prometheus metrics ignores decrements
        RestAssured.get("/micrometer/counter/-3")
                .then()
                .statusCode(200);
        assertEquals(6, getMetricValue(Integer.class, "counter", "camel-quarkus-counter"));
    }

    @Test
    public void testMicrometerCustomMetrics() throws Exception {
        //add 10 to custom component
        RestAssured.get("/micrometer/counterCustom/10")
                .then()
                .statusCode(200);

        //increment via standard component should not modify custom registry
        RestAssured.get("/micrometer/counterComposite/1")
                .then()
                .statusCode(200);
        //custom registry starts with value 10 so addition of not-custom component should not affect it
        assertEquals(10, getMetricValue(Integer.class, "counter", "camel-quarkus-custom-counter", null, 200, "custom"));

        //add 1 to custom component
        RestAssured.get("/micrometer/counterCustom/1")
                .then()
                .statusCode(200);
        //component with Quarkus's registry should still see only 1 from the first call
        assertEquals(1, getMetricValue(Integer.class, "counter", "camel-quarkus-custom-counter", null, 200, null));
        //component with custom registry should be on 11 see only 1 from the first call
        assertEquals(11, getMetricValue(Integer.class, "counter", "camel-quarkus-custom-counter", null, 200, "custom"));
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
        assertTrue(
                getMetricValue(Integer.class, "counter", "camel.exchanges.succeeded", "routeId=micrometer-metrics-timer") > 0);
        assertEquals(0, getMetricValue(Integer.class, "counter", "camel.exchanges.failed", "routeId=micrometer-metrics-timer"));
    }

    @Test
    public void testMicrometerMessageHistoryFactory() {
        RestAssured.get("/micrometer/log")
                .then()
                .statusCode(200);
        String tags = "nodeId=log1,routeId=log";
        assertTrue(getMetricValue(Double.class, "timer", "camel.message.history", tags) > 0.0);
    }

    @Test
    public void testMicrometerRouteEventNotifier() {
        assertTrue(getMetricValue(Integer.class, "gauge", "camel.routes.added") >= 4);
        assertTrue(getMetricValue(Integer.class, "gauge", "camel.routes.running") >= 4);
    }

    @Test
    public void testMicrometerExchangeEventNotifier() {
        RestAssured.get("/micrometer/log")
                .then()
                .statusCode(200);
        String tags = "endpointName=direct://log,eventType=ExchangeSentEvent";
        assertTrue(getMetricValue(Double.class, "timer", "camel.exchange.event.notifier", tags) >= 0.0);
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

    @Test
    public void testInstrumentedThreadPoolFactory() {
        assertNotNull(getMetricValue(Double.class, "timer", "executor"));
    }

    @Test
    public void testGauge() {
        RestAssured.get("/micrometer/gauge/1").then().statusCode(200);
        RestAssured.get("/micrometer/gauge/2").then().statusCode(200);
        RestAssured.get("/micrometer/gauge/4").then().statusCode(200);
        assertEquals(2.0, getMetricValue(Double.class, "gauge", "example.list.size"));
        RestAssured.get("/micrometer/gauge/6").then().statusCode(200);
        RestAssured.get("/micrometer/gauge/5").then().statusCode(200);
        RestAssured.get("/micrometer/gauge/7").then().statusCode(200);
        assertEquals(1.0, getMetricValue(Double.class, "gauge", "example.list.size"));
    }

    @Test
    public void testDumpAsJson() {
        JsonPath jsonPath = RestAssured.get("/micrometer/statistics")
                .then()
                .statusCode(200)
                .extract().jsonPath();

        //extract required values
        Map<String, Float> result = jsonPath.getMap(
                "gauges.findAll { it.id.name =~ /routes/ && it.id.tags.find { it.customTag } }.collectEntries { [it.id.name, it.value] }");

        assertEquals(result.size(), 2);
        assertTrue(result.containsKey("camel.routes.running"));
        assertEquals(7.0f, result.get("camel.routes.running"));
        assertTrue(result.containsKey("camel.routes.added"));
        assertEquals(7.0f, result.get("camel.routes.added"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "metrics", "org.apache.camel.micrometer" }) //test uses domains from both default and custom JMX registries
    @DisabledOnIntegrationTest // JMX is not supported in native mode
    public void testJMXQuarkusDomain(String domain) throws Exception {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

        ObjectName objectName = new ObjectName(domain + ":name=jvmClassesLoaded,type=gauges");
        Set<ObjectInstance> mbeans = mBeanServer.queryMBeans(objectName, null);

        assertEquals(1, mbeans.size());

        ObjectInstance oi = mbeans.iterator().next();
        Double classes = (Double) ((Attribute) mBeanServer.getAttributes(oi.getObjectName(), new String[] { "Value" }).get(0))
                .getValue();
        assertTrue(classes > 1);
    }
}
