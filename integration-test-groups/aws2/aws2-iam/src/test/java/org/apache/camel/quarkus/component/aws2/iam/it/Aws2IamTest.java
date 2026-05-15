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
package org.apache.camel.quarkus.component.aws2.iam.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.camel.quarkus.test.support.aws2.Aws2TestResource;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@QuarkusTestResource(Aws2TestResource.class)
class Aws2IamTest {

    static final String TEST_USER = "cq-test-user";
    static final String TEST_GROUP = "cq-test-group";

    @Test
    public void testIamOperations() {
        try {
            // Create user
            RestAssured.given()
                    .queryParam("userName", TEST_USER)
                    .post("/aws2-iam/users")
                    .then()
                    .statusCode(200)
                    .body(is(TEST_USER));

            // Get user
            RestAssured.given()
                    .get("/aws2-iam/users/" + TEST_USER)
                    .then()
                    .statusCode(200)
                    .body("userName", is(TEST_USER));

            // List users
            RestAssured.given()
                    .get("/aws2-iam/users")
                    .then()
                    .statusCode(200)
                    .body("$", hasItem(TEST_USER));

            // Create group
            RestAssured.given()
                    .queryParam("groupName", TEST_GROUP)
                    .post("/aws2-iam/groups")
                    .then()
                    .statusCode(200)
                    .body(is(TEST_GROUP));

            // List groups
            RestAssured.given()
                    .get("/aws2-iam/groups")
                    .then()
                    .statusCode(200)
                    .body("$", hasItem(TEST_GROUP));

            // Add user to group
            RestAssured.given()
                    .post("/aws2-iam/groups/" + TEST_GROUP + "/users/" + TEST_USER)
                    .then()
                    .statusCode(204);

            // Remove user from group
            RestAssured.given()
                    .delete("/aws2-iam/groups/" + TEST_GROUP + "/users/" + TEST_USER)
                    .then()
                    .statusCode(204);
        } finally {
            // Clean up: delete user and group
            RestAssured.given()
                    .delete("/aws2-iam/users/" + TEST_USER)
                    .then()
                    .statusCode(204);

            RestAssured.given()
                    .delete("/aws2-iam/groups/" + TEST_GROUP)
                    .then()
                    .statusCode(204);
        }
    }
}
