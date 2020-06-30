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
package org.apache.camel.quarkus.component.jolt.it;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import io.quarkus.bootstrap.util.IoUtils;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@QuarkusTest
class JoltTest {

    @Test
    public void defaultrShouldSucceed() {
        given().body("myValue").put("/jolt/defaultr").then().statusCode(200).body(is("aa-bb+myValue"));
    }

    @Test
    public void removrShouldSucceed() {
        given().body("myOtherValue").put("/jolt/removr").then().statusCode(200).body(is("2-Kept+myOtherValue"));
    }

    @Test
    public void sampleShouldSucceed() throws IOException {
        File requestBody = new File("src/test/resources/sample-input.json");
        String expectedResponseBody = IoUtils.readFile(Paths.get("src/test/resources/sample-output.json"));

        given().body(requestBody).contentType(ContentType.JSON).put("/jolt/sample").then().statusCode(200)
                .body(is(expectedResponseBody));
    }

    @Test
    public void functionShouldSucceed() throws IOException {
        File requestBody = new File("src/test/resources/function-input.json");
        String expectedResponseBody = IoUtils.readFile(Paths.get("src/test/resources/function-output.json"));

        given().body(requestBody).contentType(ContentType.JSON).put("/jolt/function").then().statusCode(200)
                .body(is(expectedResponseBody));
    }
}
