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
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import org.apache.camel.quarkus.test.support.aws2.Aws2TestResource;
import org.awaitility.Awaitility;
import org.hamcrest.Matchers;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(Aws2TestResource.class)
class Aws2LambdaTest {
    private static final Logger LOG = Logger.getLogger(Aws2LambdaTest.class);

    @Test
    public void e2e() {
        final String functionName = "cqFunction" + java.util.UUID.randomUUID().toString().replace("-", "");
        final String name = "Joe " + java.util.UUID.randomUUID().toString().replace("-", "");

        /* The role is not created immediately, so we need to retry */
        Awaitility.await()
                .pollDelay(6, TimeUnit.SECONDS) // never succeeded earlier than 6 seconds after creating the role
                .pollInterval(1, TimeUnit.SECONDS)
                .atMost(120, TimeUnit.SECONDS)
                .until(
                        () -> {
                            ExtractableResponse<?> response = RestAssured.given()
                                    .contentType("application/zip")
                                    .body(createFunctionZip())
                                    .post("/aws2-lambda/create/" + functionName)
                                    .then()
                                    .extract();
                            switch (response.statusCode()) {
                            case 201:
                                LOG.infof("Function %s created", functionName);
                                return true;
                            case 400:
                                LOG.infof("Could not create function %s yet (will retry): %d %s", functionName,
                                        response.statusCode(),
                                        response.body().asString());
                                return false;
                            default:
                                throw new RuntimeException(
                                        "Unexpected status from /aws2-lambda/create " + response.statusCode() + " "
                                                + response.body().asString());
                            }
                        });

        RestAssured.given()
                .accept(ContentType.JSON)
                .get("/aws2-lambda/listFunctions")
                .then()
                .statusCode(200)
                .body("$", Matchers.hasItem(functionName));

        /* Sometimes this does not succeed immediately */
        Awaitility.await()
                .pollDelay(200, TimeUnit.MILLISECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(120, TimeUnit.SECONDS)
                .until(
                        () -> {
                            ExtractableResponse<?> response = RestAssured.given()
                                    .contentType(ContentType.JSON)
                                    .body("{ \"firstName\": \"" + name + "\"}")
                                    .post("/aws2-lambda/invoke/" + functionName)
                                    .then()
                                    .extract();
                            String format = "Execution of aws2-lambda/invoke/%s returned status %d and content %s";
                            System.out.println(String.format(format, functionName, response.statusCode(), response.asString()));
                            switch (response.statusCode()) {
                            case 200:
                                final String greetings = response.jsonPath().getString("greetings");
                                return greetings;
                            default:
                                return null;
                            }
                        },
                        Matchers.is("Hello " + name));

        RestAssured.given()
                .delete("/aws2-lambda/delete/" + functionName)
                .then()
                .statusCode(204);

    }

    static byte[] createFunctionZip() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream out = new ZipOutputStream(baos)) {
            out.putNextEntry(new ZipEntry("index.py"));
            out.write(FUNCTION_SOURCE.getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        } catch (IOException e) {
            throw new RuntimeException("Could not create a zip file", e);
        }
        return baos.toByteArray();
    }

    private static final String FUNCTION_SOURCE = "def handler(event, context):\n" +
            "    message = 'Hello {}'.format(event['firstName'])\n" +
            "    return {\n" +
            "        'greetings' : message\n" +
            "    }\n";
}
