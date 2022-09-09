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
package org.apache.camel.quarkus.component.jslt.it;

import java.io.IOException;

import io.quarkus.test.junit.QuarkusTest;
import org.apache.camel.util.FileUtil;
import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.UTF_8;

import static io.restassured.RestAssured.given;
import static org.apache.commons.io.IOUtils.resourceToString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesRegex;

@QuarkusTest
class JsltTest {

    @Test
    public void transformInputStreamShouldSucceed() throws IOException {
        String expected = resourceToString("/demoPlayground/output.json", UTF_8);
        String input = resourceToString("/demoPlayground/input.json", UTF_8);

        given().when().body(input).get("/jslt/transformInputStream").then().statusCode(200).body(is(expected));
    }

    @Test
    public void transformInvalidBodyShouldIssueValidationErrorMessage() {
        given().when().get("/jslt/transformInvalidBody").then().statusCode(200)
                .body(matchesRegex("Allowed body types are.*"));
    }

    @Test
    public void transformStringShouldSucceed() throws IOException {
        String expected = resourceToString("/demoPlayground/output.json", UTF_8);
        String input = resourceToString("/demoPlayground/input.json", UTF_8);

        given().when().body(input).get("/jslt/transformString").then().statusCode(200).body(is(expected));
    }

    @Test
    public void transformFromHeaderWithPrettyPrintShouldSucceed() throws IOException {
        String expected = resourceToString("/demoPlayground/outputPrettyPrint.json", UTF_8);
        String input = resourceToString("/demoPlayground/input.json", UTF_8);

        if (FileUtil.isWindows()) {
            // Jackson ObjectMapper pretty printing uses platform line endings by default
            expected = expected.replace("\n", System.lineSeparator());
        }

        given().when().body(input).get("/jslt/transformFromHeaderWithPrettyPrint").then().statusCode(200).body(is(expected));
    }

    @Test
    public void transformInputStreamWithFilterShouldSucceed() throws IOException {
        String expected = resourceToString("/objectFilter/output.json", UTF_8);
        String input = resourceToString("/objectFilter/input.json", UTF_8);

        given().when().body(input).get("/jslt/transformInputStreamWithFilter").then().statusCode(200).body(is(expected));
    }

    @Test
    public void transformInputStreamWithVariablesShouldSucceed() throws IOException {
        String expected = resourceToString("/withVariables/output.json", UTF_8);
        String input = resourceToString("/withVariables/input.json", UTF_8);

        given().when().body(input).get("/jslt/transformInputStreamWithVariables").then().statusCode(200).body(is(expected));
    }

    @Test
    public void transformInputStreamWithVariablesAndPropertiesShouldSucceed() throws IOException {
        String expected = resourceToString("/withVariables/outputWithProperties.json", UTF_8);
        String input = resourceToString("/withVariables/input.json", UTF_8);

        given().when().body(input).get("/jslt/transformInputStreamWithVariablesAndProperties").then().statusCode(200)
                .body(is(expected));
    }

    @Test
    public void transformWithFunctionShouldSucceed() {
        given().get("/jslt/transformWithFunction").then().statusCode(200).body(is("1024.0"));
    }

}
