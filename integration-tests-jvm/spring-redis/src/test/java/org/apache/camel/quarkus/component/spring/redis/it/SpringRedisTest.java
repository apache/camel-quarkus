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
package org.apache.camel.quarkus.component.spring.redis.it;

import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.awaitility.Awaitility;

@QuarkusTest
@WithTestResource(SpringRedisTestResource.class)
class SpringRedisTest {

    @Test
    public void setKey() throws InterruptedException {
        RestAssured.get("/spring-redis/set")
                .then()
                .statusCode(200);

        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
            String result = RestAssured.get("/spring-redis/exists")
                    .then()
                    .statusCode(200)
                    .extract()
                    .body()
                    .asString();
            return result.equals("true");
        });
    }
}
