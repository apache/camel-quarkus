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
package org.apache.camel.quarkus.component.opentelemetry2.it;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class Opentelemetry2Test {

    static List<Map<String, String>> getSpans() {
        return RestAssured.given()
                .get("/opentelemetry2/exporter/spans")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath()
                .get();
    }

    @Test
    public void testTracedCamelRoute() throws IOException, InterruptedException {
        RestAssured.get("/opentelemetry2/trace")
                .then()
                .statusCode(200)
                .body(equalTo("Traced direct:start"));

        await().atMost(30, TimeUnit.SECONDS).pollDelay(50, TimeUnit.MILLISECONDS).until(() -> getSpans().size() == 3);
        List<Map<String, String>> spans = getSpans();
        assertEquals(3, spans.size());
        //Same trace
        assertEquals(spans.get(2).get("traceId"), spans.get(1).get("traceId"));
        assertEquals(spans.get(2).get("traceId"), spans.get(0).get("traceId"));
        // Parent relationship
        assertEquals("0000000000000000", spans.get(2).get("parentId"));
        assertEquals(spans.get(2).get("spanId"), spans.get(1).get("parentId"));
        assertEquals(spans.get(1).get("spanId"), spans.get(0).get("parentId"));
    }

}
