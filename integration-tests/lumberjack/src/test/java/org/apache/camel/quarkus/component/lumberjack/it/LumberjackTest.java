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
package org.apache.camel.quarkus.component.lumberjack.it;

import java.util.List;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.eclipse.microprofile.config.ConfigProvider;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
//@TestHTTPEndpoint(LumberjackResource.class)
@QuarkusTestResource(LumberjackTestResource.class)
class LumberjackTest {

    static final int VERSION_V2 = '2';
    static final int TYPE_ACKNOWLEDGE = 'A';

    //@Test
    public void testWithoutSSL() throws InterruptedException {
        List<LumberjackAckResponse> ackResponseList = sendPayload("camel.lumberjack.ssl.none.test-port", false);
        assertAck(ackResponseList);

        RestAssured.given()
                .get("/results/ssl/none")
                .then()
                .statusCode(200)
                .body("logs", hasSize(25))
                .body("logs[0].input_type", equalTo("log"))
                .body("logs[0].source", equalTo(
                        "/home/qatest/collectNetwork/log/data-integration/00000000-f000-0000-1541-8da26f200001/absorption.log"));
    }

    //@Test
    public void testWitSSL() throws InterruptedException {
        List<LumberjackAckResponse> ackResponseList = sendPayload("camel.lumberjack.ssl.test-port", true);
        assertAck(ackResponseList);

        RestAssured.given()
                .get("/results/ssl/route")
                .then()
                .statusCode(200)
                .body("logs", hasSize(25))
                .body("logs[0].input_type", equalTo("log"))
                .body("logs[0].source", equalTo(
                        "/home/qatest/collectNetwork/log/data-integration/00000000-f000-0000-1541-8da26f200001/absorption.log"));
    }

    //@Test
    public void testWitGlobalSSL() throws InterruptedException {
        List<LumberjackAckResponse> ackResponseList = sendPayload("camel.lumberjack.ssl.global.test-port", true);
        assertAck(ackResponseList);

        RestAssured.given()
                .get("/results/ssl/global")
                .then()
                .statusCode(200)
                .body("logs", hasSize(25))
                .body("logs[0].input_type", equalTo("log"))
                .body("logs[0].source", equalTo(
                        "/home/qatest/collectNetwork/log/data-integration/00000000-f000-0000-1541-8da26f200001/absorption.log"));
    }

    private void assertAck(List<LumberjackAckResponse> ackResponseList) {
        assertEquals(2, ackResponseList.size());
        assertEquals(VERSION_V2, ackResponseList.get(0).getVersion());
        assertEquals(VERSION_V2, ackResponseList.get(1).getVersion());
        assertEquals(TYPE_ACKNOWLEDGE, ackResponseList.get(0).getFrame());
        assertEquals(TYPE_ACKNOWLEDGE, ackResponseList.get(1).getFrame());
        assertEquals(10, ackResponseList.get(0).getSequence());
        assertEquals(15, ackResponseList.get(1).getSequence());
        assertEquals(0, ackResponseList.get(0).getRemaining());
        assertEquals(0, ackResponseList.get(1).getRemaining());
    }

    private List<LumberjackAckResponse> sendPayload(String portName, boolean withSsl) throws InterruptedException {
        final int port = ConfigProvider.getConfig().getValue(portName, Integer.class);
        List<LumberjackAckResponse> ackResponseList = LumberjackClientUtil.sendMessages(port, withSsl);
        return ackResponseList;
    }
}
