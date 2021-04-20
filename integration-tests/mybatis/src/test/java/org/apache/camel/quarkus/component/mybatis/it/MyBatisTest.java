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
package org.apache.camel.quarkus.component.mybatis.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.quarkus.component.mybatis.it.entity.Account;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
@QuarkusTestResource(H2DatabaseTestResource.class)
public class MyBatisTest {
    @Test
    public void tests() {
        testSelectOne();
        testSelectOneNotFound();
        testInsert();
        testDelete();
        testDeleteNotFound();
    }

    public void testSelectOne() {
        RestAssured.get("/mybatis/selectOne?id=456")
                .then()
                .statusCode(200)
                .body("id", equalTo(456))
                .body("firstName", equalTo("Claus"))
                .body("lastName", equalTo("Ibsen"))
                .body("emailAddress", equalTo("Noname@gmail.com"));
    }

    public void testSelectOneNotFound() {
        RestAssured.get("/mybatis/selectOne?id=999")
                .then()
                .statusCode(404);
    }

    public void testInsert() {
        Account account = new Account();
        account.setId(444);
        account.setFirstName("Willem");
        account.setLastName("Jiang");
        account.setEmailAddress("Faraway@gmail.com");

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(account)
                .post("/mybatis/insertOne")
                .then()
                .statusCode(200)
                .body(equalTo("3"));
    }

    public void testDelete() {
        RestAssured.delete("/mybatis/deleteOne?id=456")
                .then()
                .statusCode(200)
                .body(equalTo("2"));
    }

    public void testDeleteNotFound() {
        RestAssured.delete("/mybatis/deleteOne?id=999")
                .then()
                .statusCode(200)
                .body(equalTo("2"));
    }
}
