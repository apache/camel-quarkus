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
package org.apache.camel.quarkus.component.netty.udp;

import java.io.IOException;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;

@QuarkusTest
@QuarkusTestResource(NettyUdpTestResource.class)
class NettyUdpTest {

    @Test
    public void testNettyUdpProduceConsumeWithCodec() throws IOException {
        String message = "Camel Quarkus Netty Custom Codec";
        RestAssured.given()
                .body(message)
                .post("/netty/udp/codec")
                .then()
                .statusCode(200)
                .body(is("Hello Camel Quarkus Netty Custom Codec UDP"));
    }

    @Test
    public void testNettyUdpProduceConsume() throws IOException {
        RestAssured.given()
                .body("Camel Quarkus Netty")
                .post("/netty/udp")
                .then()
                .statusCode(200)
                .body(is("Hello Camel Quarkus Netty UDP"));
    }

    @Test
    public void testNettyUdpProduceConsumeWithServerInitializerFactory() throws IOException {
        RestAssured.given()
                .body("Netty")
                .post("/netty/udp/server/initializer")
                .then()
                .statusCode(200)
                .body(is("Hello Camel Quarkus Netty UDP Custom Server Initializer"));
    }

    @Test
    public void testNettyUdpProduceConsumeWithClientInitializerFactory() throws IOException {
        RestAssured.given()
                .body("Camel Quarkus Netty")
                .post("/netty/udp/client/initializer")
                .then()
                .statusCode(200)
                .body(is("Hello Camel Quarkus Netty Custom Client Initializer UDP"));
    }

    @Test
    public void testNettyUdpProduceConsumeCustomThreadPools() throws IOException {
        RestAssured.given()
                .body("Camel Quarkus Netty")
                .post("/netty/udp/custom/thread/pools")
                .then()
                .statusCode(200)
                .body(is("Hello Camel Quarkus Netty UDP Custom Worker Group"));
    }

    @Test
    public void testNettyUdpObjectSerialization() throws IOException {
        RestAssured.given()
                .body("Camel Quarkus Netty")
                .post("/netty/udp/object/serialize")
                .then()
                .statusCode(204);
    }
}
