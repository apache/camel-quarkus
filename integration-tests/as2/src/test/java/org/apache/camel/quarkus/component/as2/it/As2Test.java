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
import org.apache.camel.component.as2.api.AS2ClientManager;
import org.apache.camel.component.as2.api.AS2EncryptionAlgorithm;
import org.apache.camel.component.as2.api.entity.AS2MessageDispositionNotificationEntity;
import org.apache.camel.quarkus.component.as2.it.transport.ClientResult;
import org.apache.camel.quarkus.component.as2.it.transport.Request;
import org.apache.camel.quarkus.component.as2.it.transport.ServerResult;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
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

    private static final String EDI_MESSAGE = "UNB+UNOA:1+005435656:1+006415160:1+060515:1434+00000000000778'\n"
            + "UNH+00000000000117+INVOIC:D:97B:UN'\n"
            + "BGM+380+342459+9'\n"
            + "DTM+3:20060515:102'\n"
            + "RFF+ON:521052'\n"
            + "NAD+BY+792820524::16++CUMMINS MID-RANGE ENGINE PLANT'\n"
            + "NAD+SE+005435656::16++GENERAL WIDGET COMPANY'\n"
            + "CUX+1:USD'\n"
            + "LIN+1++157870:IN'\n"
            + "IMD+F++:::WIDGET'\n"
            + "QTY+47:1020:EA'\n"
            + "ALI+US'\n"
            + "MOA+203:1202.58'\n"
            + "PRI+INV:1.179'\n"
            + "LIN+2++157871:IN'\n"
            + "IMD+F++:::DIFFERENT WIDGET'\n"
            + "QTY+47:20:EA'\n"
            + "ALI+JP'\n"
            + "MOA+203:410'\n"
            + "PRI+INV:20.5'\n"
            + "UNS+S'\n"
            + "MOA+39:2137.58'\n"
            + "ALC+C+ABG'\n"
            + "MOA+8:525'\n"
            + "UNT+23+00000000000117'\n"
            + "UNZ+1+00000000000778'";

    @BeforeAll
    public static void setup() throws Exception {
        As2TestHelper.setup();
    }

    @Test
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
        AS2ClientManager clientManager = As2TestHelper
                .createClient(Integer.parseInt(System.getProperty(As2Resource.SERVER_PORT_PARAMETER)));

        //send message to server
        As2TestHelper.sendMessage(clientManager, EDI_MESSAGE);

        //wait for the result from the server
        await().atMost(10L, TimeUnit.SECONDS).untilAsserted(() -> {
            ServerResult result = futureResult.get();

            assertEquals(EDI_MESSAGE.replaceAll("[\n\r]", ""), result.getResult().replaceAll("[\n\r]", ""),
                    "Unexpected content for enveloped mime part");

            assertEquals(BasicHttpEntityEnclosingRequest.class.getSimpleName(), result.getRequestClass(),
                    "Request does not contain entity");
        });
    }

    @Test
    public void clientPlainTest() throws Exception {
        Request request = As2TestHelper.createClientMessageHeadersPlain();
        request.setEdiMessage(EDI_MESSAGE);
        clientTest(request);
    }

    @Test
    public void clientEncryptionPlainTest() throws Exception {
        Request request = As2TestHelper.createClientMessageHeadersEncrypted();
        request.setEdiMessage(EDI_MESSAGE);
        request.setEncryptionAlgorithm(AS2EncryptionAlgorithm.AES128_CBC);
        clientTest(request);
    }

    private void clientTest(Request request) throws Exception {

        //start server (not component)
        As2TestHelper.RequestHandler requestHandler = As2TestHelper
                .startReceiver(Integer.parseInt(System.getProperty(As2Resource.CLIENT_PORT_PARAMETER)));

        //send message by component (as client)
        ClientResult clientResult = executeRequest(request);

        //assert result
        assertNotNull(clientResult, "Response entity");
        assertEquals(2, clientResult.getPartsCount(), "Unexpected number of body parts in report");
        assertEquals(AS2MessageDispositionNotificationEntity.class.getSimpleName(), clientResult.getSecondPartClassName(),
                "Unexpected type of As2Entity");

        //assert that receiver was really used
        assertNotNull(requestHandler.getRequest(), "Request");
        assertNotNull(requestHandler.getResponse(), "Response");
    }

    private ClientResult executeRequest(Request headers) throws Exception {
        return RestAssured.given() //
                .contentType(io.restassured.http.ContentType.JSON)
                .body(headers)
                .post("/as2/client") //
                .then()
                .statusCode(200)
                .extract().body().as(ClientResult.class);
    }
}
