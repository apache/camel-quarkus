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
package org.apache.camel.quarkus.component.netty.tcp;

import java.io.IOException;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

import static org.hamcrest.Matchers.is;

@QuarkusTest
@QuarkusTestResource(NettyTcpTestResource.class)
class NettyTcpTest {

    //@Test
    public void testNettyTcpProduceConsume() throws IOException {
        RestAssured.given()
                .body("Camel Quarkus Netty")
                .post("/netty/tcp")
                .then()
                .statusCode(200)
                .body(is("Hello Camel Quarkus Netty TCP"));
    }

    //@Test
    public void testNettyTcpProduceConsumeWithByteBufResponse() throws IOException {
        RestAssured.given()
                .body("Camel Quarkus Netty")
                .post("/netty/tcp/bytebuf")
                .then()
                .statusCode(200)
                .body(is("Hello Camel Quarkus Netty TCP"));
    }

    //@Test
    public void testNettyTcpProduceConsumeWithCodec() throws IOException {
        String message = "Camel Quarkus Netty Custom Codec";
        RestAssured.given()
                .body(message)
                .post("/netty/tcp/codec")
                .then()
                .statusCode(200)
                .body(is("Hello Camel Quarkus Netty Custom Codec TCP"));
    }

    //@Test
    public void testNettyTcpSSLProduceConsume() throws IOException {
        RestAssured.given()
                .body("Camel Quarkus Netty")
                .post("/netty/tcp/ssl")
                .then()
                .statusCode(200)
                .body(is("Hello Camel Quarkus Netty TCP SSL"));
    }

    //@Test
    public void testNettyTcpProduceConsumeWithServerInitializerFactory() throws IOException {
        RestAssured.given()
                .body("Netty")
                .post("/netty/tcp/server/initializer")
                .then()
                .statusCode(200)
                .body(is("Hello Camel Quarkus Netty TCP Custom Server Initializer"));
    }

    //@Test
    public void testNettyTcpProduceConsumeWithClientInitializerFactory() throws IOException {
        RestAssured.given()
                .body("Camel Quarkus Netty")
                .post("/netty/tcp/client/initializer")
                .then()
                .statusCode(200)
                .body(is("Hello Camel Quarkus Netty TCP Custom Client Initializer"));
    }

    //@Test
    public void testNettyTcpProduceConsumeCustomThreadPools() throws IOException {
        RestAssured.given()
                .body("Camel Quarkus Netty")
                .post("/netty/tcp/custom/thread/pools")
                .then()
                .statusCode(200)
                .body(is("Hello Camel Quarkus Netty TCP Custom Worker Group"));
    }

    //@Test
    public void testNettyTcpProduceConsumeCorrelationManager() throws IOException {
        RestAssured.given()
                .post("/netty/tcp/custom/correlation/manager")
                .then()
                .statusCode(204);
    }

    //@Test
    public void testNettyTcpObjectSerialization() throws IOException {
        RestAssured.given()
                .body("Camel Quarkus Netty")
                .post("/netty/tcp/object/serialize")
                .then()
                .statusCode(204);
    }
}
