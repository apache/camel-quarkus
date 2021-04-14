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

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.apache.camel.quarkus.test.support.aws2.Aws2TestResource;
import org.awaitility.Awaitility;
import org.hamcrest.Matchers;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@QuarkusTest
@QuarkusTestResource(Aws2TestResource.class)
@EnabledIfEnvironmentVariable(named = "AWS_ACCESS_KEY", matches = "[a-zA-Z0-9]+") // TODO
                                                                                  // https://github.com/apache/camel-quarkus/issues/2216
class Aws2DdbTest {

    private static final Logger LOG = Logger.getLogger(Aws2DdbTest.class);

    @Test
    public void crud() {

        final String key = "key" + UUID.randomUUID().toString().replace("-", "");
        final String msg = "val" + UUID.randomUUID().toString().replace("-", "");

        /* Ensure initially empty */
        RestAssured.get("/aws2-ddb/item/" + key)
                .then()
                .statusCode(204);

        /* Put */
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(msg)
                .post("/aws2-ddb/item/" + key)
                .then()
                .statusCode(201);

        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(120, TimeUnit.SECONDS).until(
                () -> {
                    ExtractableResponse<Response> result = RestAssured.get("/aws2-ddb/item/" + key)
                            .then()
                            .statusCode(Matchers.anyOf(Matchers.is(200), Matchers.is(204)))
                            .extract();
                    LOG.info("Expecting " + msg + " got " + result.statusCode() + ": " + result.body().asString());
                    return result.body().asString();
                },
                Matchers.is(msg));

        /* Update */
        final String newMsg = "newVal" + UUID.randomUUID().toString().replace("-", "");
        RestAssured.given()
                .body(newMsg)
                .put("/aws2-ddb/item/" + key)
                .then()
                .statusCode(204);
        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(120, TimeUnit.SECONDS).until(
                () -> {
                    ExtractableResponse<Response> result = RestAssured.get("/aws2-ddb/item/" + key)
                            .then()
                            .statusCode(Matchers.anyOf(Matchers.is(200), Matchers.is(204)))
                            .extract();
                    LOG.info("Expecting " + newMsg + " got " + result.statusCode() + ": " + result.body().asString());
                    return result.body().asString();
                },
                Matchers.is(newMsg));

        /* Delete */
        RestAssured.given()
                .delete("/aws2-ddb/item/" + key)
                .then()
                .statusCode(204);

        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(120, TimeUnit.SECONDS).until(
                () -> {
                    ExtractableResponse<Response> result = RestAssured.get("/aws2-ddb/item/" + key)
                            .then()
                            .extract();
                    LOG.info("Expecting " + msg + " got " + result.statusCode() + ": " + result.body().asString());
                    return result.statusCode();
                },
                Matchers.is(204));

        /* The above actions should trigger the following three change events */
        RestAssured.get("/aws2-ddbstream/change")
                .then()
                .statusCode(200)
                .body("key", Matchers.is(key))
                .body("new", Matchers.is(msg));
        RestAssured.get("/aws2-ddbstream/change")
                .then()
                .statusCode(200)
                .body("key", Matchers.is(key))
                .body("old", Matchers.is(msg))
                .body("new", Matchers.is(newMsg));
        RestAssured.get("/aws2-ddbstream/change")
                .then()
                .statusCode(200)
                .body("key", Matchers.is(key))
                .body("old", Matchers.is(newMsg));

    }

}
