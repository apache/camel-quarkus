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
package org.apache.camel.quarkus.component.jsonata.it;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.apache.commons.io.IOUtils;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@QuarkusTest
class JsonataTest {

    //@Test
    public void test() throws IOException {
        String input = IOUtils.toString(getClass().getResourceAsStream("/input.json"), StandardCharsets.UTF_8);
        String expected = IOUtils.toString(getClass().getResourceAsStream("/output.json"), StandardCharsets.UTF_8);
        given().contentType(ContentType.TEXT).body(input).get("/jsonata/transform").then().statusCode(200).body(is(expected));
    }

}
