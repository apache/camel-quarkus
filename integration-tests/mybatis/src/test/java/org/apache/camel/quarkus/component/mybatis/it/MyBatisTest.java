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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        testSelectList();
        testUpdateOne();
        testUpdateList();
        testInsert();
        testInsertList();
        testInsertRollback();
        testSelectOneNotFound();
        testDelete();
        testDeleteNotFound();
        testDeleteList();
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

    public void testSelectList() {
        RestAssured.get("/mybatis/selectList")
                .then()
                .statusCode(200)
                .body("size()", equalTo(2))
                .body("[0].id", equalTo(123))
                .body("[0].firstName", equalTo("James"))
                .body("[0].lastName", equalTo("Strachan"))
                .body("[0].emailAddress", equalTo("TryGuessing@gmail.com"))
                .body("[1].id", equalTo(456))
                .body("[1].firstName", equalTo("Claus"))
                .body("[1].lastName", equalTo("Ibsen"))
                .body("[1].emailAddress", equalTo("Noname@gmail.com"));
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

    public void testInsertRollback() {
        Account account = new Account();
        account.setId(999);
        account.setFirstName("Rollback");
        account.setLastName("Rollback");
        account.setEmailAddress("Rollback@gmail.com");

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(account)
                .post("/mybatis/insertOne")
                .then()
                .statusCode(500);
    }

    public void testInsertList() {
        Account account1 = new Account();
        account1.setId(555);
        account1.setFirstName("Aaron");
        account1.setLastName("Daubman");
        account1.setEmailAddress("ReadTheDevList@gmail.com");

        Account account2 = new Account();
        account2.setId(666);
        account2.setFirstName("Amos");
        account2.setLastName("Feng");
        account2.setEmailAddress("ZHENG@gmail.com");

        List<Account> accountList = new ArrayList<>(2);

        accountList.add(account1);
        accountList.add(account2);

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(accountList)
                .post("/mybatis/insertList")
                .then()
                .statusCode(200)
                .body(equalTo("5"));
    }

    public void testUpdateOne() {
        Account account = new Account();
        account.setId(456);
        account.setFirstName("Claus");
        account.setLastName("Ibsen");
        account.setEmailAddress("Other@gmail.com");

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(account)
                .patch("/mybatis/updateOne")
                .then()
                .statusCode(200)
                .body(equalTo("2"));

        RestAssured.get("/mybatis/selectOne?id=456")
                .then()
                .statusCode(200)
                .body("id", equalTo(456))
                .body("firstName", equalTo("Claus"))
                .body("lastName", equalTo("Ibsen"))
                .body("emailAddress", equalTo("Other@gmail.com"));
    }

    public void testUpdateList() {
        Account account1 = new Account();
        account1.setId(123);

        Account account2 = new Account();
        account2.setId(456);

        Map<String, Object> params = new HashMap<>();
        params.put("list", Arrays.asList(account1, account2));
        params.put("emailAddress", "Update@gmail.com");

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(params)
                .patch("/mybatis/updateList")
                .then()
                .statusCode(200)
                .body(equalTo("2"));

        RestAssured.get("/mybatis/selectList")
                .then()
                .statusCode(200)
                .body("size()", equalTo(2))
                .body("[0].id", equalTo(123))
                .body("[0].firstName", equalTo("James"))
                .body("[0].lastName", equalTo("Strachan"))
                .body("[0].emailAddress", equalTo("Update@gmail.com"))
                .body("[1].id", equalTo(456))
                .body("[1].firstName", equalTo("Claus"))
                .body("[1].lastName", equalTo("Ibsen"))
                .body("[1].emailAddress", equalTo("Update@gmail.com"));
    }

    public void testDelete() {
        RestAssured.delete("/mybatis/deleteOne?id=456")
                .then()
                .statusCode(200)
                .body(equalTo("4"));
    }

    public void testDeleteNotFound() {
        RestAssured.delete("/mybatis/deleteOne?id=999")
                .then()
                .statusCode(200)
                .body(equalTo("4"));
    }

    public void testDeleteList() {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Arrays.asList(444, 555, 666))
                .delete("/mybatis/deleteList")
                .then()
                .statusCode(200)
                .body(equalTo("1"));
    }
}
