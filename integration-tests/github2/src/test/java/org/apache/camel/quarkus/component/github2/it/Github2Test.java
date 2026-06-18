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
package org.apache.camel.quarkus.component.github2.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@QuarkusTestResource(Github2TestResource.class)
class Github2Test {

    @Test
    public void testGetCommitFile() {
        RestAssured.get("/github2/get")
                .then()
                .statusCode(200)
                .body(containsString("Apache Camel extensions for Quarkus"));
    }

    @Test
    public void testRepositoryDeserialization() {
        RestAssured.get("/github2/repository")
                .then()
                .statusCode(200)
                .body(is("apache/camel-quarkus"));
    }

    @Test
    public void testCommitDeserialization() {
        RestAssured.get("/github2/commit/author")
                .then()
                .statusCode(200)
                .body(is("test"));
    }
}
