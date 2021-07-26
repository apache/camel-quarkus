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
package org.apache.camel.quarkus.component.qute.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

import static org.hamcrest.Matchers.is;

@QuarkusTest
class QuteTest {

    //@Test
    public void testTemplate() {
        RestAssured.get("/qute/template/World")
                .then()
                .body(is("Hello World"));
    }

    //@Test
    public void testInvalidTemplatePath() {
        RestAssured.get("/qute/template/invalid/path")
                .then()
                .body(is("Unable to parse Qute template from path: invalid-path"));
    }

    //@Test
    public void tesTemplateContentFromHeader() {
        RestAssured.given()
                .body("Hello {body}")
                .post("/qute/template")
                .then()
                .body(is("Hello World"));
    }
}
