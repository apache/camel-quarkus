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
package org.apache.camel.quarkus.component.as2.it;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.camel.component.as2.api.entity.AS2MessageDispositionNotificationEntity;
import org.apache.camel.quarkus.component.as2.it.transport.ClientResult;
import org.apache.camel.quarkus.component.as2.it.transport.Request;
import org.apache.camel.quarkus.component.as2.it.transport.ServerResult;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTestResource(As2TestResource.class)
@QuarkusTest
public class As2Test {
    private static final Logger LOG = LoggerFactory.getLogger(As2Test.class);

    //@Test
    public void serverPlainTest() throws Exception {

        //prepare component by sending empty request with no wait
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Boolean> initResult = executor.submit(
                () -> RestAssured.given().get("/as2/serverInit").then().statusCode(200).extract().as(Boolean.class));

        //give some time for server component to be created
        await().atMost(5L, TimeUnit.SECONDS).untilAsserted(() -> {
            assertTrue(initResult.get());
        });

        //execute component server to wait for the result
        Future<ServerResult> futureResult = executor.submit(
                () -> RestAssured.given().get("/as2/server").then().statusCode(200).extract().as(ServerResult.class));

        //create client for sending message to server
        As2Sender.As2SenderClient client = As2Sender
                .createClient(ConfigProvider.getConfig().getValue(As2Resource.SERVER_PORT_PARAMETER, Integer.class));

        //send message to server
        client.sendMessage(As2Helper.EDI_MESSAGE);

        //wait for the result from the server
        await().atMost(10L, TimeUnit.SECONDS).untilAsserted(() -> {
            ServerResult result = futureResult.get();

            assertEquals(As2Helper.EDI_MESSAGE.replaceAll("[\n\r]", ""), result.getResult().replaceAll("[\n\r]", ""),
                    "Unexpected content for enveloped mime part");

            assertEquals(BasicHttpEntityEnclosingRequest.class.getSimpleName(), result.getRequestClass(),
                    "Request does not contain entity");
        });
    }

    //@Test
    public void clientPlainTest() throws Exception {
        clientTest(As2Helper.createPlainRequest());
    }

    //@Test
    public void clientEncryptionTest() throws Exception {
        clientTest(As2Helper.createEncryptedRequest());
    }

    //@Test
    public void clientMultipartSignedTest() throws Exception {
        ClientResult clientResult = clientTest(As2Helper.createMultipartSignedRequest());

        Assertions.assertTrue(clientResult.isSignedEntityReceived(), "Signature Entity");
    }

    private ClientResult clientTest(Request request) throws Exception {

        //start server (not component)
        As2Receiver.RequestHandler requestHandler = As2Receiver
                .startReceiver(ConfigProvider.getConfig().getValue(As2Resource.CLIENT_PORT_PARAMETER, Integer.class));

        //send message by component (as client)
        ClientResult clientResult = RestAssured.given() //
                .contentType(io.restassured.http.ContentType.JSON)
                .body(request)
                .post("/as2/client") //
                .then()
                .statusCode(200)
                .extract().body().as(ClientResult.class);

        //assert result
        assertNotNull(clientResult, "Response entity");
        assertEquals(2, clientResult.getPartsCount(), "Unexpected number of body parts in report");
        assertEquals(AS2MessageDispositionNotificationEntity.class.getSimpleName(), clientResult.getSecondPartClassName(),
                "Unexpected type of As2Entity");

        //assert that receiver was really used
        assertNotNull(requestHandler.getRequest(), "Request");
        assertNotNull(requestHandler.getResponse(), "Response");

        return clientResult;
    }
}
