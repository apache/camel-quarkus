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

@QuarkusTest
abstract class AbstractMicrometerTest {

    <T> T getMetricValue(Class<T> as, String type, String name) {
        return getMetricValue(as, type, name, null);
    }

    <T> T getMetricValue(Class<T> as, String type, String name, String tags) {
        return getMetricValue(as, type, name, tags, 200);
    }

    <T> T getMetricValue(Class<T> as, String type, String name, String tags, int statusCode) {
        return getMetricValue(as, type, name, tags, statusCode, null);
    }

    <T> T getMetricValue(Class<T> as, String type, String name, String tags, int statusCode, String registry) {
        String r = (registry == null ? "standard" : registry);

        ResponseBodyExtractionOptions resp = RestAssured.given()
                .queryParam("tags", tags)
                .when()
                .get("/micrometer/metric/" + type + "/" + name + "/" + r)
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
    String dumpMetrics() {
        return RestAssured.get("/q/metrics")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();
    }
}
