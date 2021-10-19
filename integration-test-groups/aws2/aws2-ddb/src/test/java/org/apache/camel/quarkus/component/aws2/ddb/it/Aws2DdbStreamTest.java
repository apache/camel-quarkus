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
package org.apache.camel.quarkus.component.aws2.ddb.it;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.apache.camel.ServiceStatus;
import org.apache.camel.quarkus.test.support.aws2.Aws2TestResource;
import org.awaitility.Awaitility;
import org.hamcrest.Matchers;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import static org.apache.camel.quarkus.component.aws2.ddb.it.Aws2DdbResource.Table;

@QuarkusTest
@QuarkusTestResource(Aws2TestResource.class)
class Aws2DdbStreamTest {

    private static final Logger LOG = Logger.getLogger(Aws2DdbStreamTest.class);

    @Test
    public void stream() {
        final String key1 = "key-1-" + UUID.randomUUID().toString().replace("-", "");
        final String msg1 = "val-1" + UUID.randomUUID().toString().replace("-", "");
        final String key2 = "key-2-" + UUID.randomUUID().toString().replace("-", "");
        final String msg2 = "val-2-" + UUID.randomUUID().toString().replace("-", "");
        final String key3 = "key-3-" + UUID.randomUUID().toString().replace("-", "");
        final String msg3 = "val-3-" + UUID.randomUUID().toString().replace("-", "");

        routeController("start", null);
        routeController("status", ServiceStatus.Started.name());

        waitForStreamConsumerToStart();

        /* Put #1 */
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(msg1)
                .queryParam("table", Table.stream)
                .post("/aws2-ddb/item/" + key1)
                .then()
                .statusCode(201);

        /* Update #1 */
        final String newMsg = "newVal" + UUID.randomUUID().toString().replace("-", "");
        RestAssured.given()
                .body(newMsg)
                .queryParam("table", Table.stream)
                .put("/aws2-ddb/item/" + key1)
                .then()
                .statusCode(204);

        /* PUT #2 */
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(msg2)
                .queryParam("table", Table.stream)
                .post("/aws2-ddb/item/" + key2)
                .then()
                .statusCode(201);

        StringBuilder put2SeqNumber = new StringBuilder();

        Awaitility.await().atMost(120, TimeUnit.SECONDS).until(
                () -> {
                    ExtractableResponse<Response> result = RestAssured.get("/aws2-ddbstream/change")
                            .then()
                            .statusCode(200)
                            .extract();

                    List<Map> retVal = result.jsonPath().getList("$", Map.class);
                    LOG.info("Expecting 3 (+ 1 init) events got " + result.statusCode() + ": " + result.body().asString());
                    //remove init events
                    retVal = retVal.stream().filter(m -> !String.valueOf(m.get("key")).startsWith("initKey"))
                            .collect(Collectors.toList());

                    if (retVal.size() == 3 && retVal.get(2) != null && retVal.get(2).get("sequenceNumber") != null) {
                        put2SeqNumber.append(((String) retVal.get(2).get("sequenceNumber")));
                    }
                    return retVal;
                },
                /* The above actions should trigger the following three change events (initEvent is also present) */
                list -> list.size() == 3

                        && key1.equals(list.get(0).get("key"))
                        && msg1.equals(list.get(0).get("new"))

                        && key1.equals(list.get(1).get("key"))
                        && msg1.equals(list.get(1).get("old"))
                        && newMsg.equals(list.get(1).get("new"))

                        && key2.equals(list.get(2).get("key"))
                        && msg2.equals(list.get(2).get("new"))

                        && put2SeqNumber.length() > 0);

        RestAssured.given()
                .get("/aws2-ddbstream/clear")
                .then()
                .statusCode(204);

        /* Restart route and clear results */
        routeController("stop", null);
        routeController("status", ServiceStatus.Stopped.name());

        routeController("start", null);
        routeController("status", ServiceStatus.Started.name());
        waitForStreamConsumerToStart();

        /* Put #3 */
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(msg3)
                .queryParam("table", Table.stream)
                .post("/aws2-ddb/item/" + key3)
                .then()
                .statusCode(201);

        /* Delete #3 */
        RestAssured.given()
                .queryParam("table", Table.stream)
                .delete("/aws2-ddb/item/" + key3)
                .then()
                .statusCode(204);

        /* There should be put & update events for key 3 */
        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(120, TimeUnit.SECONDS).until(
                () -> {
                    ExtractableResponse<Response> result = RestAssured.get("/aws2-ddbstream/change")
                            .then()
                            .statusCode(200)
                            .extract();

                    List<Map> retVal = result.jsonPath().getList("$", Map.class);

                    //remove init events
                    retVal = retVal.stream().filter(m -> !String.valueOf(m.get("key")).startsWith("initKey"))
                            .collect(Collectors.toList());

                    LOG.info("Expecting 2 events, got " + result.statusCode() + ": " + retVal);

                    return retVal;
                },
                list -> list.size() == 2
                        && key3.equals(list.get(0).get("key"))
                        && msg3.equals(list.get(0).get("new"))

                        && key3.equals(list.get(1).get("key"))
                        && msg3.equals(list.get(1).get("old")));
    }

    private void waitForStreamConsumerToStart() {
        final String initKeyPrefix = "initKey";
        final String initMsg = "val";

        /* Wait for the consumer to start. Test event has to be created periodically to ensure consumer reception */
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(initMsg)
                .queryParam("table", Table.stream)
                .post("/aws2-ddb/item/" + initKeyPrefix + UUID.randomUUID().toString().replace("-", ""))
                .then()
                .statusCode(201);

        /* Periodically check that init event is received and if nothing is received, invoke another one  */
        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(120, TimeUnit.SECONDS).until(
                () -> {
                    ExtractableResponse<Response> result = RestAssured.get("/aws2-ddbstream/change")
                            .then()
                            .statusCode(200)
                            .extract();

                    LOG.info("Expecting at least 1 init event, got " + result.statusCode() + ": " + result.body().asString());

                    List<Map> res = result.jsonPath().getList("$", Map.class);

                    /* If there is no received event, consumer is still not running. Repeat insert. */
                    if (res.isEmpty()) {
                        RestAssured.given()
                                .contentType(ContentType.TEXT)
                                .body(initMsg)
                                .queryParam("table", Table.stream)
                                .post("/aws2-ddb/item/initKey" + UUID.randomUUID().toString().replace("-", ""))
                                .then()
                                .statusCode(201);
                    }
                    return res;
                },
                /* If at least one of the init events is recceived, consumer is working */
                list -> !list.isEmpty());
    }

    private String routeController(String operation, String expectedResult) {
        String routeId = "aws2DdbStreamRoute";
        if (expectedResult == null) {
            RestAssured.given()
                    .get("/aws2-ddbstream/route/" + routeId + "/" + operation)
                    .then().statusCode(204);
        } else {
            Awaitility.await().atMost(5, TimeUnit.SECONDS).until(
                    () -> RestAssured
                            .get("/aws2-ddbstream/route/" + routeId + "/" + operation)
                            .then()
                            .statusCode(200)
                            .extract().asString(),
                    Matchers.is(expectedResult));
        }

        return null;
    }
}
