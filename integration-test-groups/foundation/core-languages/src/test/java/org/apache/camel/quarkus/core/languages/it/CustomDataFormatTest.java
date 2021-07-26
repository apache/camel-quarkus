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
package org.apache.camel.quarkus.core.languages.it;

import io.quarkus.test.junit.QuarkusTest;
import org.hamcrest.Matchers;

import static io.restassured.RestAssured.given;

@QuarkusTest
public class CustomDataFormatTest {
    //@Test
    public void customDataFormat() {
        given()
                .body("foo bar")
                .post("/core-languages/route/customDataFormatMarshal/String")
                .then()
                .statusCode(200)
                .body(Matchers.is("(foo bar)"));

        given()
                .body("(bar baz)")
                .post("/core-languages/route/customDataFormatUnmarshal/byte[]")
                .then()
                .statusCode(200)
                .body(Matchers.is("bar baz"));
    }

}
