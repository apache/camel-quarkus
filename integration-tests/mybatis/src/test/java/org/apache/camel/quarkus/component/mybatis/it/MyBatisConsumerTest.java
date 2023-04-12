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
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
@QuarkusTestResource(H2DatabaseTestResource.class)
public class MyBatisConsumerTest {

    @Test
    public void testSelectList() {
        RestAssured.get("/mybatis/consumer")
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
}
