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
package org.apache.camel.quarkus.component.bindy.it;

import java.util.ArrayList;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.quarkus.component.bindy.it.model.Header;
import org.apache.camel.quarkus.component.bindy.it.model.MessageOrder;
import org.apache.camel.quarkus.component.bindy.it.model.Security;
import org.apache.camel.quarkus.component.bindy.it.model.Trailer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class MessageRecordTest {

    private static final String MESSAGE_ORDER = "1=BE.CHM.0018=BEGIN9=2010=22022=458=camel - quarkus - bindy test\r\n";

    @Test
    public void jsonToMessageShouldSucceed() {
        MessageOrder order = new MessageOrder();
        order.setAccount("BE.CHM.001");
        order.setHeader(new Header());
        order.getHeader().setBeginString("BEGIN");
        order.getHeader().setBodyLength(20);
        order.setSecurities(new ArrayList<>());
        order.getSecurities().add(new Security());
        order.getSecurities().get(0).setIdSource("4");
        order.setText("camel - quarkus - bindy test");
        order.setTrailer(new Trailer());
        order.getTrailer().setCheckSum(220);

        String messageOrder = RestAssured.given() //
                .contentType(ContentType.JSON).body(order).get("/bindy/jsonToMessage").then().statusCode(200).extract()
                .asString();
        assertEquals(MESSAGE_ORDER, messageOrder);
    }

    @Test
    public void messageToJsonShouldSucceed() {
        MessageOrder order = RestAssured.given() //
                .contentType(ContentType.TEXT).body(MESSAGE_ORDER).get("/bindy/messageToJson").then().statusCode(200).extract()
                .as(MessageOrder.class);

        assertNotNull(order);
        assertEquals("BE.CHM.001", order.getAccount());
        assertNotNull(order.getHeader());
        assertEquals("BEGIN", order.getHeader().getBeginString());
        assertEquals(20, order.getHeader().getBodyLength());
        assertNotNull(order.getSecurities());
        assertEquals(1, order.getSecurities().size());
        assertEquals("4", order.getSecurities().get(0).getIdSource());
        assertEquals("camel - quarkus - bindy test", order.getText());
        assertNotNull(order.getTrailer());
        assertEquals(220, order.getTrailer().getCheckSum());
    }
}
