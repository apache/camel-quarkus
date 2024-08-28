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

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import org.apache.camel.quarkus.component.mybatis.it.entity.Account;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@QuarkusTestResource(H2DatabaseTestResource.class)
public class MyBatisConsumerTest {

    @Test
    public void testSelectList() {
        RestAssured.get("/mybatis/consumer")
                .then()
                .statusCode(200)
                .body(is("2"));

        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(10, TimeUnit.SECONDS).until(() -> {
            final JsonPath body = given().get("/mybatis/afterConsumer").then().extract().body().jsonPath();
            if (body != null) {
                List<Account> accounts = body.getList("", Account.class);
                return accounts.size() == 2 &&
                        accounts.get(0).getId() == 123 &&
                        accounts.get(0).getFirstName().equals("James") &&
                        accounts.get(0).getLastName().equals("Strachan") &&
                        accounts.get(0).getEmailAddress().equals("TryGuessing@gmail.com") &&
                        accounts.get(1).getId() == 456 &&
                        accounts.get(1).getFirstName().equals("Claus") &&
                        accounts.get(1).getLastName().equals(("Ibsen")) &&
                        accounts.get(1).getEmailAddress().equals("Noname@gmail.com");

            } else {
                return false;
            }
        });
    }
}
