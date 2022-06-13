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
package org.apache.camel.quarkus.test.extensions.continousDev;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class HelloET extends CamelQuarkusTestSupport {

    @Test
    public void hello1Test() throws Exception {
        Files.createDirectories(testDirectory());
        Path testFile = testFile("hello.txt");
        Files.write(testFile, "Hello ".getBytes(StandardCharsets.UTF_8));

        RestAssured.given()
                .body(fileUri() + "?fileName=hello.txt")
                .post("/hello/message")

                .then()
                .statusCode(200)
                .body(is("Hello Sheldon"));

    }

    @Test
    public void hello2Test() throws Exception {
        Files.createDirectories(testDirectory());
        Path testFile = testFile("hello.txt");
        Files.write(testFile, "Hello ".getBytes(StandardCharsets.UTF_8));

        RestAssured.given()
                .body(fileUri() + "?fileName=hello.txt")
                .post("/hello/message")

                .then()
                .statusCode(200)
                .body(is("Hello Leonard"));
    }

    @Test
    public void hello3Test() throws Exception {
        Files.createDirectories(testDirectory());
        Path testFile = testFile("hello.txt");
        Files.write(testFile, "Hello ".getBytes(StandardCharsets.UTF_8));

        RestAssured.given()
                .body(fileUri() + "?fileName=hello.txt")
                .post("/hello/message")

                .then()
                .statusCode(200)
                .body(is("Hello Leonard"));
    }

}
