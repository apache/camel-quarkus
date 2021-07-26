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
package org.apache.camel.quarkus.component.aws2.sqs.it;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.quarkus.test.support.aws2.Aws2TestResource;
import org.awaitility.Awaitility;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;

import static org.hamcrest.core.Is.is;

@QuarkusTest
@QuarkusTestResource(Aws2TestResource.class)
class Aws2SqsSnsTest {

    //@Test
    public void sqs() {
        final Config config = ConfigProvider.getConfig();
        final String queueName = config.getValue("aws-sqs.queue-name", String.class);

        String[] queues = RestAssured.get("/aws2-sqs-sns/sqs/queues")
                .then()
                .statusCode(200)
                .extract()
                .body().as(String[].class);
        Assertions.assertTrue(Stream.of(queues).anyMatch(url -> url.contains(queueName)));

        final String msg = "sqs" + UUID.randomUUID().toString().replace("-", "");
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(msg)
                .post("/aws2-sqs-sns/sqs/send")
                .then()
                .statusCode(201);

        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(120, TimeUnit.SECONDS).until(
                () -> RestAssured.get("/aws2-sqs-sns/sqs/receive").then().statusCode(200).extract().body().asString(),
                Matchers.is(msg));

    }

    //@Test
    void sns() {
        final String snsMsg = "sns" + UUID.randomUUID().toString().replace("-", "");
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(snsMsg)
                .post("/aws2-sqs-sns/sns/send")
                .then()
                .statusCode(201);

        RestAssured
                .get("/aws2-sqs-sns/sns/receiveViaSqs")
                .then()
                .statusCode(200)
                .body("Message", is(snsMsg));

    }

}
