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

import java.io.IOException;
import java.util.Locale;
import java.util.function.BiFunction;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.KeyedDataQueue;
import com.ibm.as400.access.MessageQueue;
import com.ibm.as400.access.ObjectDoesNotExistException;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.camel.component.jt400.Jt400Constants;
import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.microprofile.config.ConfigProvider;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@QuarkusTest
@EnabledIfEnvironmentVariable(named = "JT400_URL", matches = ".+")
public class Jt400Test {

    @BeforeAll
    public static void beforeAll() throws Exception {
        //read all messages from the queues to be sure that they are empty

        //clear reply-to message queue
        clearQueue("cq.jt400.message-replyto-queue",
                (as400, path) -> {
                    try {
                        return new MessageQueue(as400, path).receive(null);
                    } catch (Exception e) {
                        return null;
                    }
                });

        //clear  message queue
        clearQueue("cq.jt400.message-queue",
                (as400, path) -> {
                    try {
                        return new MessageQueue(as400, path).receive(null);
                    } catch (Exception e) {
                        return null;
                    }
                });

        //clear  keyed queue for key1
        clearQueue("cq.jt400.message-queue",
                (as400, path) -> {
                    try {
                        return new KeyedDataQueue(as400, path).read("key1");
                    } catch (Exception e) {
                        return null;
                    }
                });

        //clear  keyed queue for key2
        clearQueue("cq.jt400.message-queue",
                (as400, path) -> {
                    try {
                        return new KeyedDataQueue(as400, path).read("key1");
                    } catch (Exception e) {
                        return null;
                    }
                });
    }

    @Test
    public void testDataQueue() {
        String msg = RandomStringUtils.randomAlphanumeric(10).toLowerCase(Locale.ROOT);

        RestAssured.given()
                .body(msg)
                .post("/jt400/dataQueue/write")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("Hello " + msg));

        RestAssured.post("/jt400/dataQueue/read")
                .then()
                .statusCode(200)
                .body("result", Matchers.equalTo("Hello " + msg));
    }

    @Test
    public void testDataQueueBinary() {
        String msg = RandomStringUtils.randomAlphanumeric(10).toLowerCase(Locale.ROOT);

        RestAssured.given()
                .body(msg)
                .post("/jt400/dataQueue/write")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("Hello " + msg));

        RestAssured.given()
                .queryParam("format", "binary")
                .post("/jt400/dataQueue/read")
                .then()
                .statusCode(200)
                .body("result", Matchers.equalTo("Hello " + msg));
    }

    @Test
    public void testKeyedDataQueue() {
        String msg1 = RandomStringUtils.randomAlphanumeric(10).toLowerCase(Locale.ROOT);
        String msg2 = RandomStringUtils.randomAlphanumeric(10).toLowerCase(Locale.ROOT);
        String key1 = "key1";
        String key2 = "key2";

        RestAssured.given()
                .body(msg1)
                .queryParam("key", key1)
                .post("/jt400/dataQueue/write/")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("Hello " + msg1));

        RestAssured.given()
                .body("Sheldon2")
                .queryParam("key", key2)
                .post("/jt400/dataQueue/write/")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("Hello Sheldon2"));

        RestAssured.given()
                .body(key1)
                .post("/jt400/dataQueue/read/")
                .then()
                .statusCode(200)
                .body("result", Matchers.equalTo("Hello " + msg1))
                .body(Jt400Constants.KEY, Matchers.equalTo(key1));

        RestAssured.given()
                .body(key1)
                .queryParam("searchType", "NE")
                .post("/jt400/dataQueue/read/")
                .then()
                .statusCode(200)
                .body("result", Matchers.not(Matchers.equalTo("Hello " + msg2)))
                .body(Jt400Constants.KEY, Matchers.equalTo(key2));
    }

    @Test
    public void testMessageQueue() throws AS400SecurityException, ObjectDoesNotExistException, IOException,
            InterruptedException, ErrorCompletingRequestException {
        String msg = RandomStringUtils.randomAlphanumeric(10).toLowerCase(Locale.ROOT);

        RestAssured.given()
                .body(msg)
                .post("/jt400/messageQueue/write")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("Hello " + msg));

        RestAssured.post("/jt400/messageQueue/read")
                .then()
                .statusCode(200)
                .body("result", Matchers.is("Hello " + msg))
                //check of headers
                .body(Jt400Constants.SENDER_INFORMATION, Matchers.not(Matchers.empty()))
                .body(Jt400Constants.MESSAGE_FILE, Matchers.is(""))
                .body(Jt400Constants.MESSAGE_SEVERITY, Matchers.is(0))
                .body(Jt400Constants.MESSAGE_ID, Matchers.is(""))
                .body(Jt400Constants.MESSAGE_TYPE, Matchers.is(4))
                .body(Jt400Constants.MESSAGE, Matchers.is("QueuedMessage: Hello " + msg));
        //Jt400Constants.MESSAGE_DFT_RPY && Jt400Constants.MESSAGE_REPLYTO_KEY are used only for a special
        // type of message which can not be created by the camel component (*INQUIRY)
    }

    @Test
    public void testInquiryMessageQueue() throws AS400SecurityException, ObjectDoesNotExistException, IOException,
            InterruptedException, ErrorCompletingRequestException {
        String msg = RandomStringUtils.randomAlphanumeric(10).toLowerCase(Locale.ROOT);

        //sending a message using the same client as component
        RestAssured.given()
                .body(msg)
                .post("/jt400/client/inquiryMessage/write")
                .then()
                .statusCode(200);

        RestAssured.given()
                .body(ConfigProvider.getConfig().getValue("cq.jt400.message-replyto-queue", String.class))
                .post("/jt400/client/queuedMessage/read")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("reply to: " + msg));
    }

    @Test
    public void testProgramCall() {
        RestAssured.given()
                .body("test")
                .post("/jt400/programCall")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("hello camel"));
    }

    private static void clearQueue(String queue, BiFunction<AS400, String, Object> readFromQueue) {
        String jt400Url = ConfigProvider.getConfig().getValue("cq.jt400.url", String.class);
        String jt400Username = ConfigProvider.getConfig().getValue("cq.jt400.username", String.class);
        String jt400Password = ConfigProvider.getConfig().getValue("cq.jt400.password", String.class);
        String jt400Library = ConfigProvider.getConfig().getValue("cq.jt400.library", String.class);
        String jt400MessageQueue = ConfigProvider.getConfig().getValue(queue, String.class);

        String objectPath = String.format("/QSYS.LIB/%s.LIB/%s", jt400Library, jt400MessageQueue);

        AS400 as400 = new AS400(jt400Url, jt400Username, jt400Password);

        int i = 0;
        Object msg = null;
        //read messages until null is received
        do {
            msg = readFromQueue.apply(as400, objectPath);
        } while (i++ < 10 && msg != null);

        if (i == 10 && msg != null) {
            throw new IllegalStateException("There is a message present in a queue!");
        }
    }

}
