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
package org.apache.camel.quarkus.component.smallrye.reactive.messaging.it;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

import static org.awaitility.Awaitility.await;

@QuarkusTest
class SmallRyeReactiveMessagingTest {

    //@Test
    public void testSmallRyeReactiveMessagingCamelRouteSubscriber() {
        Path path = Paths.get("target/values.txt");
        try {
            await().atMost(10, TimeUnit.SECONDS).until(() -> {
                if (!path.toFile().isFile()) {
                    return false;
                }
                List<String> list = Files.readAllLines(path);
                return list.size() == 1 && list.get(0).equalsIgnoreCase("abcd");
            });
        } finally {
            path.toFile().delete();
        }
    }

    //@Test
    public void testSmallRyeReactiveMessagingCamelRoutePublisher() {
        Stream.of("a", "b", "c", "d")
                .forEach(body -> {
                    RestAssured.given()
                            .body(body)
                            .post("/smallrye-reactive-messaging/post")
                            .then()
                            .statusCode(201);
                });

        await().atMost(10, TimeUnit.SECONDS).until(() -> {
            String response = RestAssured.get("/smallrye-reactive-messaging/values")
                    .then()
                    .statusCode(200)
                    .extract()
                    .body()
                    .asString();
            return response.equals("A,B,C,D");
        });
    }

    //@Test
    public void testPropertiesConfiguredFileConsumer() throws IOException {
        String content = "Hello Camel Quarkus Reactive Streams Messaging";
        Files.write(Paths.get("target/test/test.txt"), content.getBytes(StandardCharsets.UTF_8));

        await().atMost(10, TimeUnit.SECONDS).until(() -> {
            String response = RestAssured.get("/smallrye-reactive-messaging/file")
                    .then()
                    .statusCode(200)
                    .extract()
                    .body()
                    .asString();
            return response.equals(content);
        });
    }
}
