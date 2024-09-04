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
package org.apache.camel.quarkus.component.flink.it;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.awaitility.Awaitility;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;

@QuarkusTest
class FlinkTest {

    @Test
    public void dataSetCallback() throws IOException {
        Path path = Files.createTempFile("fileDataSet", ".txt");
        try {
            String text = "foo\n"
                    + "bar\n"
                    + "baz\n"
                    + "qux\n"
                    + "quux";
            Files.writeString(path, text);
            RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .post("/flink/dataset/{filePath}", path.toAbsolutePath().toString())
                    .then()
                    .statusCode(200)
                    .and()
                    .body(greaterThanOrEqualTo("5"));

        } finally {
            try {
                Files.deleteIfExists(path);
            } catch (Exception e) {
                // Do nothing
            }
        }
    }

    @Test
    public void dataStreamCallback() throws IOException {
        Path path = Files.createTempFile("fileDataStream", ".txt");
        try {
            String text = "Hello!!Camel flink!";
            Awaitility.await().atMost(10, TimeUnit.SECONDS)
                    .until(
                            () -> RestAssured.given()
                                    .contentType(ContentType.TEXT)
                                    .body(text)
                                    .post("/flink/datastream/{filePath}", path.toAbsolutePath().toString())
                                    .then()
                                    .statusCode(200)
                                    .extract()
                                    .body().asString(),
                            Matchers.is("384"));

        } finally {
            try {
                Files.deleteIfExists(path);
            } catch (Exception e) {
                // Do nothing
            }

        }
    }
}
