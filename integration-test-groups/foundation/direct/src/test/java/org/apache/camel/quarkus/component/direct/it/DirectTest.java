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
package org.apache.camel.quarkus.component.direct.it;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;

@QuarkusTest
public class DirectTest {
    //@Test
    public void catalogComponent() throws IOException {
        RestAssured.when().get("/direct/catalog/component/direct").then().body(not(emptyOrNullString()));
    }

    //@Test
    public void routeTemplate() {
        RestAssured.when().get("/direct/routes/template/myTemplate/World").then().body(is("Hello World"));
        RestAssured.when().get("/direct/routes/template/myTemplate/Earth").then().body(is("Hello Earth"));
    }

    //@Test
    public void directConsumerProducer() {
        final String message1 = UUID.randomUUID().toString();
        final String message2 = UUID.randomUUID().toString();
        RestAssured.given()
                .body(message1)
                .post("/direct/route/to-logger")
                .then()
                .statusCode(204);
        RestAssured.given()
                .body(message2)
                .post("/direct/route/consumer-producer")
                .then()
                .statusCode(204);

        await().atMost(10L, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS).until(() -> {
            String log = new String(Files.readAllBytes(Paths.get("target/quarkus.log")), StandardCharsets.UTF_8);
            return log.contains(message1) && log.contains(message2);
        });
    }

}
