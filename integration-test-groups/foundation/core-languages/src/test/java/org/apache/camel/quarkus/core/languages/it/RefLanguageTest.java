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

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

/**
 * Note that a similar test is performed in {@code camel-quarkus-integration-test-ref}. The difference is that here we
 * test without the {@code camel-quarkus-ref} dependency because the Ref language is brought through
 * {@code camel-quarkus-core}.
 */
@QuarkusTest
public class RefLanguageTest {
    //@Test
    public void ref() {
        given()
                .body("My very foo")
                .post("/core-languages/route/refLanguage/String")
                .then()
                .statusCode(200)
                .body(is("MY VERY FOO"));
    }

}
