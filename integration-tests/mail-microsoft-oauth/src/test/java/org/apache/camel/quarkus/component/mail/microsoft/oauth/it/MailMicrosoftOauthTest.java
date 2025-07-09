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
package org.apache.camel.quarkus.component.mail.microsoft.oauth.it;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import jakarta.json.bind.JsonbBuilder;
import org.apache.camel.ServiceStatus;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "CQ_MAIL_MICROSOFT_OAUTH_USERNAME", matches = ".+")
@EnabledIfEnvironmentVariable(named = "CQ_MAIL_MICROSOFT_OAUTH_CLIENT_ID", matches = ".+")
@EnabledIfEnvironmentVariable(named = "CQ_MAIL_MICROSOFT_OAUTH_CLIENT_SECRET", matches = ".+")
@EnabledIfEnvironmentVariable(named = "CQ_MAIL_MICROSOFT_OAUTH_TENANT_ID", matches = ".+")
@QuarkusTest
class MailMicrosoftOauthTest {

    @SuppressWarnings("unchecked")
    @Test
    public void sendAndReceive() {

        final String content = "Test email!" + UUID.randomUUID();

        //send an email
        MailMicrosoftOauthUtil.sendMessage(MailMicrosoftOauthRoute.TEST_SUBJECT, content);

        //start route
        startRoute("receiverRoute");

        //receive
        Awaitility.await().pollInterval(2, TimeUnit.SECONDS).atMost(2, TimeUnit.MINUTES).until(() -> {
            //receive
            return (List<Map<String, String>>) JsonbBuilder.create()
                    .fromJson(RestAssured.get("/mail-microsoft-oauth/getReceived/")
                            .then()
                            .statusCode(200)
                            .extract().body().asString(), List.class);
        }, list -> list.size() == 1
                && list.get(0).get("content").contains(content));
    }

    private void startRoute(String name) {
        RestAssured.given()
                .get("/mail-microsoft-oauth/route/" + name + "/start")
                .then().statusCode(204);

        //wait for finish
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(
                () -> {
                    String status = RestAssured
                            .get("/mail-microsoft-oauth/route/" + name + "/status")
                            .then()
                            .statusCode(200)
                            .extract().asString();

                    return status.equals(ServiceStatus.Started.name());
                });
    }
}
