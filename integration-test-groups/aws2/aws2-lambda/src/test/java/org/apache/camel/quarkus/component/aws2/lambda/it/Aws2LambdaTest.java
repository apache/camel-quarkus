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
package org.apache.camel.quarkus.component.aws2.lambda.it;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import org.apache.camel.quarkus.test.support.aws2.Aws2TestResource;
import org.apache.camel.quarkus.test.support.aws2.BaseAWs2TestSupport;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
@QuarkusTestResource(Aws2TestResource.class)
class Aws2LambdaTest extends BaseAWs2TestSupport {
    private static final Logger LOG = Logger.getLogger(Aws2LambdaTest.class);

    public Aws2LambdaTest() {
        super("/aws2-lambda");
    }

    @Test
    public void performingOperationsOnLambdaFunctionShouldSucceed() {
        final String functionName = "cqFunction" + java.util.UUID.randomUUID().toString().replace("-", "");

        // The required role to create a function is not created immediately, so we need to retry
        await().pollDelay(6, TimeUnit.SECONDS) // never succeeded earlier than 6 seconds after creating the role
                .pollInterval(1, TimeUnit.SECONDS)
                .atMost(120, TimeUnit.SECONDS)
                .until(() -> {
                    ExtractableResponse<?> response = RestAssured.given()
                            .contentType("application/zip")
                            .body(createInitialLambdaFunctionZip())
                            .post("/aws2-lambda/function/create/" + functionName)
                            .then()
                            .extract();
                    switch (response.statusCode()) {
                    case 201:
                        LOG.infof("Lambda function %s created", functionName);
                        return true;
                    case 400:
                        LOG.infof("Could not create Lambda function %s yet (will retry): %d %s", functionName,
                                response.statusCode(), response.body().asString());
                        return false;
                    default:
                        throw new RuntimeException("Unexpected status from /aws2-lambda/function/create "
                                + response.statusCode() + " " + response.body().asString());
                    }
                });

        String functionArn = RestAssured.given()
                .accept(ContentType.JSON)
                .get("/aws2-lambda/function/getArn/" + functionName)
                .then()
                .statusCode(200)
                .extract().asString();
        assertNotNull(functionArn);

        getUpdateListAndInvokeFunctionShouldSucceed(functionName);
        createGetDeleteAndListAliasShouldSucceed(functionName);
        createListDeleteFunctionTagsShouldSucceed(functionName, functionArn);
        publishAndListVersionShouldSucceed(functionName);
        createListAndDeleteEventSourceMappingShouldSucceed(functionName);

        RestAssured.given()
                .delete("/aws2-lambda/function/delete/" + functionName)
                .then()
                .statusCode(204);
    }

    @Override
    public void testMethodForDefaultCredentialsProvider() {
        RestAssured.given()
                .get("/aws2-lambda/function/list/")
                .then()
                .statusCode(200);
    }

    public void getUpdateListAndInvokeFunctionShouldSucceed(String functionName) {
        final String name = "Joe " + java.util.UUID.randomUUID().toString().replace("-", "");

        await().pollDelay(1L, TimeUnit.SECONDS)
                .pollInterval(1L, TimeUnit.SECONDS)
                .atMost(120, TimeUnit.SECONDS)
                .until(() -> {
                    String state = RestAssured.given()
                            .get("/aws2-lambda/function/getState/" + functionName)
                            .then()
                            .statusCode(200)
                            .extract().asString();

                    if (!"Active".equals(state)) {
                        String format = "The function with name '%s' has state '%s', so retrying";
                        LOG.infof(format, functionName, state);
                        return false;
                    } else {
                        String format = "The function with name '%s' has state 'Active', so moving to next step";
                        LOG.infof(format, functionName);
                        return true;
                    }
                });

        RestAssured.given()
                .contentType("application/zip")
                .body(createUpdatedLambdaFunctionZip())
                .put("/aws2-lambda/function/update/" + functionName)
                .then()
                .statusCode(200);

        RestAssured.given()
                .accept(ContentType.JSON)
                .get("/aws2-lambda/function/list")
                .then()
                .statusCode(200)
                .body("$", hasItem(functionName));

        /* Sometimes this does not succeed immediately */
        await().pollDelay(200, TimeUnit.MILLISECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(120, TimeUnit.SECONDS)
                .until(() -> {
                    ExtractableResponse<?> response = RestAssured.given()
                            .contentType(ContentType.JSON)
                            .body("{ \"firstName\": \"" + name + "\"}")
                            .post("/aws2-lambda/function/invoke/" + functionName)
                            .then()
                            .extract();
                    String format = "Execution of aws2-lambda/invoke/%s returned status %d and content %s";
                    LOG.infof(format, functionName, response.statusCode(), response.asString());
                    switch (response.statusCode()) {
                    case 200:
                        final String greetings = response.jsonPath().getString("greetings");
                        return greetings;
                    default:
                        return null;
                    }
                }, is("Hello updated " + name));
    }

    public void createGetDeleteAndListAliasShouldSucceed(String functionName) {

        String functionVersion = "$LATEST";
        String aliasName = "alias_LATEST_" + functionName;

        RestAssured.given()
                .queryParam("functionName", functionName)
                .queryParam("functionVersion", functionVersion)
                .queryParam("aliasName", aliasName)
                .post("/aws2-lambda/alias/create/")
                .then()
                .statusCode(201);

        RestAssured.given()
                .queryParam("functionName", functionName)
                .queryParam("aliasName", aliasName)
                .get("/aws2-lambda/alias/get/")
                .then()
                .statusCode(200)
                .body(is("$LATEST"));

        RestAssured.given()
                .queryParam("functionName", functionName)
                .queryParam("aliasName", aliasName)
                .delete("/aws2-lambda/alias/delete")
                .then()
                .statusCode(204);

        RestAssured.given()
                .queryParam("functionName", functionName)
                .accept(ContentType.JSON)
                .get("/aws2-lambda/alias/list")
                .then()
                .statusCode(200)
                .body("$", not(hasItem(aliasName)));
    }

    public void createListDeleteFunctionTagsShouldSucceed(String functionName, String functionArn) {

        String uuid = java.util.UUID.randomUUID().toString().replace("-", "");
        String tagResourceKey = functionName + "-tagKey-" + uuid;
        String tagResourceValue = functionName + "-tagValue-" + uuid;

        RestAssured.given()
                .queryParam("functionArn", functionArn)
                .queryParam("tagResourceKey", tagResourceKey)
                .queryParam("tagResourceValue", tagResourceValue)
                .post("/aws2-lambda/tag/create")
                .then()
                .statusCode(201);

        RestAssured.given()
                .queryParam("functionArn", functionArn)
                .get("/aws2-lambda/tag/list")
                .then()
                .statusCode(200)
                .body(tagResourceKey, is(tagResourceValue));

        RestAssured.given()
                .queryParam("functionArn", functionArn)
                .queryParam("tagResourceKey", tagResourceKey)
                .delete("/aws2-lambda/tag/delete")
                .then()
                .statusCode(204);

        RestAssured.given()
                .queryParam("functionArn", functionArn)
                .get("/aws2-lambda/tag/list")
                .then()
                .statusCode(200)
                .body(tagResourceKey, is(emptyOrNullString()));
    }

    public void publishAndListVersionShouldSucceed(String functionName) {

        RestAssured.given()
                .queryParam("functionName", functionName)
                .get("/aws2-lambda/version/list")
                .then()
                .statusCode(200)
                .body("$", not(hasItem("1")));

        RestAssured.given()
                .queryParam("functionName", functionName)
                .post("/aws2-lambda/version/publish")
                .then()
                .statusCode(201);

        RestAssured.given()
                .queryParam("functionName", functionName)
                .get("/aws2-lambda/version/list")
                .then()
                .statusCode(200)
                .body("$", hasItem("1"));
    }

    public void createListAndDeleteEventSourceMappingShouldSucceed(String functionName) {
        String eventSourceMappingUuid = RestAssured.given()
                .queryParam("functionName", functionName)
                .post("/aws2-lambda/event-source-mapping/create")
                .then()
                .statusCode(201)
                .extract().asString();
        assertNotNull(eventSourceMappingUuid);

        await().pollDelay(10L, TimeUnit.SECONDS)
                .pollInterval(1L, TimeUnit.SECONDS)
                .atMost(120, TimeUnit.SECONDS)
                .until(() -> {
                    Map<String, String> eventSourceMappingStatuses = RestAssured.given()
                            .queryParam("functionName", functionName)
                            .get("/aws2-lambda/event-source-mapping/list")
                            .then()
                            .statusCode(200)
                            .extract().jsonPath().getMap("$", String.class, String.class);

                    if (!eventSourceMappingStatuses.containsKey(eventSourceMappingUuid)) {
                        LOG.infof("Found no event source mapping with id '%s', so retrying", eventSourceMappingUuid);
                        return false;
                    }
                    String status = eventSourceMappingStatuses.get(eventSourceMappingUuid);
                    if (!"Enabled".equals(status)) {
                        String format = "The event source mapping with id '%s' has status '%s', so retrying";
                        LOG.infof(format, eventSourceMappingUuid, status);
                        return false;
                    } else {
                        String format = "The event source mapping with id '%s' has status 'Enabled', so moving to next step";
                        LOG.infof(format, eventSourceMappingUuid);
                        return true;
                    }
                });

        RestAssured.given()
                .queryParam("eventSourceMappingUuid", eventSourceMappingUuid)
                .delete("/aws2-lambda/event-source-mapping/delete")
                .then()
                .statusCode(204);

        await().pollDelay(16L, TimeUnit.SECONDS)
                .pollInterval(1L, TimeUnit.SECONDS)
                .atMost(120, TimeUnit.SECONDS)
                .until(() -> {
                    Map<String, String> eventSourceMappingStatuses = RestAssured.given()
                            .queryParam("functionName", functionName)
                            .get("/aws2-lambda/event-source-mapping/list")
                            .then()
                            .statusCode(200)
                            .extract().jsonPath().getMap("$", String.class, String.class);

                    if (eventSourceMappingStatuses.containsKey(eventSourceMappingUuid)) {
                        String format = "The event source mapping with id '%s' is still present with status '%s', so retrying";
                        LOG.infof(format, eventSourceMappingUuid, eventSourceMappingStatuses.get(eventSourceMappingUuid));
                        return false;
                    }
                    return true;
                });
    }

    static byte[] createInitialLambdaFunctionZip() {
        return createLambdaFunctionZip(INITIAL_FUNCTION_SOURCE);
    }

    static byte[] createUpdatedLambdaFunctionZip() {
        return createLambdaFunctionZip(UPDATED_FUNCTION_SOURCE);
    }

    static byte[] createLambdaFunctionZip(String source) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream out = new ZipOutputStream(baos)) {
            out.putNextEntry(new ZipEntry("index.py"));
            out.write(source.getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        } catch (IOException e) {
            throw new RuntimeException("Could not create a zip file", e);
        }
        return baos.toByteArray();
    }

    private static final String INITIAL_FUNCTION_SOURCE = "def handler(event, context):\n"
            + "    message = 'Hello {}'.format(event['firstName'])\n"
            + "    return {\n"
            + "        'greetings' : message\n"
            + "    }\n";

    private static final String UPDATED_FUNCTION_SOURCE = "def handler(event, context):\n"
            + "    message = 'Hello updated {}'.format(event['firstName'])\n"
            + "    return {\n"
            + "        'greetings' : message\n" +
            "    }\n";
}
