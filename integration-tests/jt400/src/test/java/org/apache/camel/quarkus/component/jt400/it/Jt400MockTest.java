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
package org.apache.camel.quarkus.component.jt400.it;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.quarkus.component.jt400.it.mock.Jt400MockResource;
import org.apache.camel.util.CollectionHelper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

@QuarkusTest
@DisabledIfSystemProperty(named = "skip-mock-tests", matches = "true")

public class Jt400MockTest {

    @Test
    public void testReadKeyedDataQueue() {
        prepareMockReply(Jt400MockResource.ReplyType.ok);
        prepareMockReply(Jt400MockResource.ReplyType.DQRequestAttributesNormal, CollectionHelper.mapOf("keyLength", 5));
        prepareMockReply(Jt400MockResource.ReplyType.ok);
        prepareMockReply(Jt400MockResource.ReplyType.DQReadNormal, 0x8003, "mocked jt400", "Hello from mocked jt400!", "MYKEY");

        RestAssured.get("/jt400/mock/keyedDataQueue/read")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("Hello from mocked jt400!"));
    }

    @Test
    public void testWriteKeyedDataQueue() {
        prepareMockReply(Jt400MockResource.ReplyType.ok);
        prepareMockReply(Jt400MockResource.ReplyType.DQRequestAttributesNormal, CollectionHelper.mapOf("keyLength", 7));
        prepareMockReply(Jt400MockResource.ReplyType.ok);
        prepareMockReply(Jt400MockResource.ReplyType.DQCommonReply, CollectionHelper.mapOf("hashCode", 0x8002));

        RestAssured.given()
                .body("Written in mocked jt400!")
                .post("/jt400/mock/keyedDataQueue/write/testKey")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("Written in mocked jt400!"));
    }

    @Test
    public void testReadMessageQueue() {
        prepareMockReply(Jt400MockResource.ReplyType.RCExchangeAttributesReply);
        prepareMockReply(Jt400MockResource.ReplyType.RCExchangeAttributesReply);
        prepareMockReply(Jt400MockResource.ReplyType.RCCallProgramReply);

        RestAssured.get("/jt400/mock/messageQueue/read")
                .then()
                .statusCode(200);
    }

    @Test
    public void testWriteMessageQueue() {
        prepareMockReply(Jt400MockResource.ReplyType.RCExchangeAttributesReply);
        prepareMockReply(Jt400MockResource.ReplyType.RCExchangeAttributesReply);
        prepareMockReply(Jt400MockResource.ReplyType.RCCallProgramReply);

        RestAssured.given()
                .body("Written in mocked jt400!")
                .post("/jt400/mock/messageQueue/write/testKey")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("Written in mocked jt400!"));
    }

    @Test
    public void testProgramCall() {
        prepareMockReply(Jt400MockResource.ReplyType.RCExchangeAttributesReply);
        prepareMockReply(Jt400MockResource.ReplyType.RCExchangeAttributesReply);
        prepareMockReply(Jt400MockResource.ReplyType.RCCallProgramReply);

        RestAssured.given()
                .body("Written in mocked jt400!")
                .post("/jt400/mock/programCall")
                .then()
                .statusCode(200)
                .body(Matchers.both(Matchers.not(Matchers.containsString("par1"))).and(
                        Matchers.containsString("par2")));
    }

    private void prepareMockReply(Jt400MockResource.ReplyType replyType,
            Integer hashCode,
            String senderInformation,
            String entry,
            String key) {
        //prepare mock data
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(CollectionHelper.mapOf("replyType", replyType.name(),
                        "hashCode", hashCode,
                        "senderInformation", senderInformation,
                        "entry", entry,
                        "key", key))
                .post("/jt400/mock/put/mockResponse")
                .then()
                .statusCode(200);
    }

    private void prepareMockReply(Jt400MockResource.ReplyType replyType) {
        //prepare mock data
        RestAssured.given()
                .body(CollectionHelper.mapOf("replyType", replyType.name()))
                .contentType(ContentType.JSON)
                .post("/jt400/mock/put/mockResponse")
                .then()
                .statusCode(200);
    }

    private void prepareMockReply(Jt400MockResource.ReplyType replyType, Map<String, Object> data) {
        Map<String, Object> request = new HashMap<>(data);
        request.put("replyType", replyType.name());
        //prepare mock data
        RestAssured.given()
                .body(request)
                .contentType(ContentType.JSON)
                .post("/jt400/mock/put/mockResponse")
                .then()
                .statusCode(200);
    }
}
