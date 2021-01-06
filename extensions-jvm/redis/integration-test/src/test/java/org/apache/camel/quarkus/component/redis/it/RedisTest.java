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
package org.apache.camel.quarkus.component.redis.it;

import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.post;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@QuarkusTestResource(RedisTestResource.class)
class RedisTest {

    @Test
    public void aggregateUsingRedisPersistentRepositoryShouldSucceed() {
        post("/redis/aggregate/{message}/{correlationKey}", "A", 1).then().statusCode(204);
        post("/redis/aggregate/{message}/{correlationKey}", "B", 1).then().statusCode(204);
        post("/redis/aggregate/{message}/{correlationKey}", "F", 2).then().statusCode(204);
        post("/redis/aggregate/{message}/{correlationKey}", "C", 1).then().statusCode(204);

        await().atMost(5L, TimeUnit.SECONDS).until(() -> {
            return given().contentType(ContentType.JSON).get("/redis/get-aggregates").path("size()").equals(1);
        });
        String[] results = given().contentType(ContentType.JSON).get("/redis/get-aggregates").then().extract()
                .as(String[].class);
        assertEquals(1, results.length);
        assertEquals("ABC", results[0]);
    }

}
