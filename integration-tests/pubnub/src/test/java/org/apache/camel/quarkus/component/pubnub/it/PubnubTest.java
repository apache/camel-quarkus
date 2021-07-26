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
package org.apache.camel.quarkus.component.pubnub.it;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.camel.quarkus.test.wiremock.MockServer;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;

@QuarkusTest
@QuarkusTestResource(PubnubTestResource.class)
class PubnubTest {

    @MockServer
    WireMockServer server;

    //@Test
    public void testPublishSubscribeShouldReturnSameMessage() {
        String message = "Test PubNub publish";

        if (server != null) {
            server.stubFor(post(urlPathMatching("/publish/(.*)/(.*)/0/test/0"))
                    .willReturn(aResponse().withStatus(200).withBody("[1,\"Sent\",\"14598111595318003\"]")));

            server.stubFor(get(urlPathMatching("/v2/subscribe/(.*)/(.*)/0"))
                    .willReturn(aResponse()
                            .withBody(
                                    "{\"t\":{\"t\":\"14607577960932487\",\"r\":1},\"m\":[{\"a\":\"4\",\"f\":0,\"i\":\"Publisher-A\",\"p\":{\"t\":\"14607577960925503\",\"r\":1},\"o\":"
                                            + "{\"t\":\"14737141991877032\",\"r\":2},\"k\":\"sub-c-4cec9f8e-01fa-11e6-8180-0619f8945a4f\","
                                            + "\"c\":\"test\",\"d\":\"" + message + "\",\"b\":\"test\"}]}")));

            server.stubFor(get(urlPathMatching("/v2/presence/sub-key/(.*)/channel/test/heartbeat"))
                    .willReturn(aResponse().withBody("{\"status\": 200, \"message\": \"OK\", \"service\": \"Presence\"}")));

            server.stubFor(get(urlPathMatching("/v2/presence/sub-key/(.*)/channel/test/leave"))
                    .willReturn(aResponse().withBody("{\"status\": 200, \"message\": \"OK\", \"service\": \"Presence\"}")));
        }

        // Publish message
        RestAssured.given()
                .body(message)
                .post("/pubnub/publish/test")
                .then()
                .statusCode(201);

        // Subscribe and consume the message
        RestAssured.get("/pubnub/subscribe")
                .then()
                .statusCode(200)
                .body(is(message));
    }

    //@Test
    public void testPubNubFire() {
        String message = "Test PubNub fire";

        if (server != null) {
            server.stubFor(get(urlPathMatching("/publish/(.*)/(.*)/0/(.*)/0/%22Test%20PubNub%20fire%22"))
                    .willReturn(aResponse().withBody("[1,\"Sent\",\"14598111595318003\"]")));
        }

        RestAssured.given()
                .body(message)
                .post("/pubnub/fire")
                .then()
                .statusCode(200)
                .body(matchesPattern("[0-9]+"));
    }

    //@Test
    public void testPubNubPresence() {
        if (server != null) {
            server.stubFor(get(urlPathMatching("/v2/subscribe/(.*)/(.*)/0"))
                    .willReturn(aResponse()
                            .withBody(
                                    "{\"t\":{\"t\":\"16127893527303416\",\"r\":12},\"m\":[{\"a\":\"4\",\"f\":0,\"p\":{\"t\":\"16127893527306385\","
                                            + "\"r\":12},\"k\":\"sub-c-b14e2458-66d0-11eb-b373-323c3659f3c9\",\"c\":\"test-pnpres\","
                                            + "\"u\":{\"pn_action\":\"join\",\"pn_uuid\":\"pn-cd0e849f-71ec-4006-a989-c0cc7d1b37a5\","
                                            + "\"pn_timestamp\":1612789352,\"pn_occupancy\":1,\"pn_ispresence\":1,\"pn_channel\":\"test\"},"
                                            + "\"d\":{\"action\":\"join\",\"uuid\":\"pn-cd0e849f-71ec-4006-a989-c0cc7d1b37a5\",\"timestamp\":1612789352,\"occupancy\":1},"
                                            + "\"b\":\"test-pnpres\"}]}\n")));

            server.stubFor(get(urlPathMatching("/v2/presence/sub-key/(.*)/channel/test/leave"))
                    .willReturn(aResponse().withBody("{\"status\": 200, \"message\": \"OK\", \"service\": \"Presence\"}")));
        }

        RestAssured.given()
                .get("/pubnub/presence")
                .then()
                .statusCode(200)
                .body(is("test"));
    }

    //@Test
    public void testPubNubState() {
        if (server != null) {
            server.stubFor(get(urlPathMatching("/v2/presence/sub-key/(.*)/channel/(.*)/uuid/myuuid/data"))
                    .willReturn(aResponse().withBody(
                            "{\"status\": 200, \"message\": \"OK\", \"payload\": {\"test-state-key\": \"test-state-value\"}, \"service\": \"Presence\"}")));

            server.stubFor(get(urlPathMatching("/v2/presence/sub-key/(.*)/channel/(.*)/uuid/(.*)"))
                    .willReturn(aResponse().withBody(
                            "{\"status\": 200, \"message\": \"OK\", \"payload\": {\"test-state-key\": \"test-state-value\"}, "
                                    + "\"uuid\": \"pn-a2b8fef1-5c4f-4f26-b71b-0065eede6ad7\", \"channel\": \"test-state\", \"service\": \"Presence\"}")));
        }

        RestAssured.given()
                .post("/pubnub/state")
                .then()
                .statusCode(204);

        RestAssured.given()
                .get("/pubnub/state")
                .then()
                .statusCode(200)
                .body(is("test-state-value"));
    }

    //@Test
    public void testPubNubHistory() {
        if (server != null) {
            server.stubFor(post(urlPathMatching("/publish/(.*)/(.*)/0/(.*)/0"))
                    .willReturn(aResponse().withStatus(200).withBody("[1,\"Sent\",\"14598111595318003\"]")));

            server.stubFor(get(urlPathMatching("/v2/history/sub-key/(.*)/channel/(.*)"))
                    .willReturn(aResponse().withBody("[[\"Test PubNub history\"],16124441622202741,16127935855440082]")));
        }

        RestAssured.given()
                .body("Test PubNub history")
                .post("/pubnub/publish/test-history")
                .then()
                .statusCode(201);

        RestAssured.given()
                .get("/pubnub/history")
                .then()
                .statusCode(200)
                .body(matchesPattern("^[1-9][0-9]*$"));
    }

    //@Test
    public void testPubNubHereNow() {
        if (server != null) {
            server.stubFor(post(urlPathMatching("/publish/(.*)/(.*)/0/(.*)/0"))
                    .willReturn(aResponse().withStatus(200).withBody("[1,\"Sent\",\"14598111595318003\"]")));

            server.stubFor(get(urlPathMatching("/v2/presence/sub_key/(.*)/channel/(.*)"))
                    .willReturn(aResponse().withBody(
                            "{\"status\": 200, \"message\": \"OK\", \"occupancy\": 0, \"uuids\": [], \"service\": \"Presence\"}")));
        }

        RestAssured.given()
                .body("Test PubNub Here Now")
                .post("/pubnub/publish/test-herenow")
                .then()
                .statusCode(201);

        RestAssured.given()
                .get("/pubnub/herenow")
                .then()
                .statusCode(200)
                .body(matchesPattern("^[1-9][0-9]*$"));
    }
}
