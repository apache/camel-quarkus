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

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import com.ibm.as400.access.QueuedMessage;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.camel.component.jt400.Jt400Constants;
import org.apache.commons.lang3.RandomStringUtils;
import org.awaitility.Awaitility;
import org.hamcrest.Matchers;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@QuarkusTest
@EnabledIfEnvironmentVariable(named = "JT400_URL", matches = ".+")
@QuarkusTestResource(Jt400TestResource.class)
public class Jt400Test {
    private static final Logger LOGGER = Logger.getLogger(Jt400Test.class);

    private final int MSG_LENGTH = 20;
    //tests may be executed in parallel, therefore the timeout is a little bigger in case the test has to wait for another one
    private static final int WAIT_IN_SECONDS = 30;

    @BeforeAll
    public static void beforeAll() throws Exception {
        //lock execution
        getClientHelper().lock();

        //for development purposes
        //        logQueues();

        //clear al data in advance to be sure that there is no data in the queues
        //it is not possible to clear data after the run because of CPF2451 Message queue REPLYMSGQ is allocated to another job
        //wait is required also because of CPF2451, usually takes ~20 seconds to release connections to a  reply queue
        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(WAIT_IN_SECONDS, TimeUnit.SECONDS).until(
                () -> {
                    try {
                        return getClientHelper().clear();
                    } catch (Exception e) {
                        LOGGER.debug("Clear failed because of: " + e.getMessage());
                        return false;
                    }
                },
                Matchers.is(true));
    }

    private static void logQueues() throws Exception {
        StringBuilder sb = new StringBuilder("\n");
        sb.append("************************************************************");
        sb.append(getClientHelper().dumpQueues());
        sb.append("\n**********************************************************\n");
        LOGGER.info(sb.toString());
    }

    @Test
    public void testDataQueue() {
        LOGGER.debug("**** testDataQueue() ** has started ");

        String msg = RandomStringUtils.secure().nextAlphanumeric(MSG_LENGTH).toLowerCase(Locale.ROOT);
        String answer = "Hello From DQ: " + msg;

        RestAssured.given()
                .body(msg)
                .post("/jt400/dataQueue/write")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo(answer));

        LOGGER.debug("testDataQueue: message '" + answer + "' was written. ");

        getClientHelper().registerForRemoval(Jt400TestResource.RESOURCE_TYPE.lifoQueueu, answer);

        RestAssured.post("/jt400/dataQueue/read")
                .then()
                .statusCode(200)
                .body("result", Matchers.equalTo(answer));
    }

    @Test
    public void testDataQueueBinary() throws Exception {
        LOGGER.debug("**** testDataQueueBinary() ** has started ");
        String msg = RandomStringUtils.secure().nextAlphanumeric(MSG_LENGTH).toLowerCase(Locale.ROOT);
        String answer = "Hello (bin) " + msg;

        RestAssured.given()
                .body(msg)
                .queryParam("format", "binary")
                .post("/jt400/dataQueue/write")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo(answer));

        LOGGER.debug("testDataQueueBinary: message '" + answer + "' was written. ");

        //register to delete
        getClientHelper().registerForRemoval(Jt400TestResource.RESOURCE_TYPE.lifoQueueu, answer);

        RestAssured.given()
                .queryParam("format", "binary")
                .post("/jt400/dataQueue/read")
                .then()
                .statusCode(200)
                .body("result", Matchers.equalTo(answer));
    }

    @Test
    public void testKeyedDataQueue() {
        LOGGER.debug("**** testKeyedDataQueue() ** has started ");
        String msg1 = RandomStringUtils.secure().nextAlphanumeric(MSG_LENGTH).toLowerCase(Locale.ROOT);
        String msg2 = RandomStringUtils.secure().nextAlphanumeric(MSG_LENGTH).toLowerCase(Locale.ROOT);
        String answer1 = "Hello From KDQ: " + msg1;
        String answer2 = "Hello From KDQ: " + msg2;

        String key1 = RandomStringUtils.secure().nextAlphanumeric(MSG_LENGTH - 1).toLowerCase(Locale.ROOT);
        //key2 is right after key1
        String key2 = key1 + "a";

        RestAssured.given()
                .body(msg1)
                .queryParam("key", key1)
                .post("/jt400/dataQueue/write/")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo(answer1));

        LOGGER.debug("testKeyedDataQueue: message '" + answer1 + " (key " + key1 + ") was written. ");
        getClientHelper().registerForRemoval(Jt400TestResource.RESOURCE_TYPE.keyedDataQue, key1);

        RestAssured.given()
                .body(msg2)
                .queryParam("key", key2)
                .post("/jt400/dataQueue/write/")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo(answer2));

        LOGGER.debug("testKeyedDataQueue: message '" + answer2 + " (key " + key2 + ") was written. ");
        getClientHelper().registerForRemoval(Jt400TestResource.RESOURCE_TYPE.keyedDataQue, key2);

        RestAssured.given()
                .body(key1)
                .post("/jt400/dataQueue/read/")
                .then()
                .statusCode(200)
                .body("result", Matchers.equalTo(answer1))
                .body(Jt400Constants.KEY, Matchers.equalTo(key1));

        RestAssured.given()
                .body(key1)
                .queryParam("searchType", "GE")
                .post("/jt400/dataQueue/read/")
                .then()
                .statusCode(200)
                .body("result", Matchers.not(Matchers.equalTo(answer1)))
                .body(Jt400Constants.KEY, Matchers.equalTo(key2));
    }

    @Test
    public void testMessageQueue() throws Exception {
        LOGGER.debug("**** testMessageQueue() ** has started ");
        //write
        String msg = RandomStringUtils.secure().nextAlphanumeric(MSG_LENGTH).toLowerCase(Locale.ROOT);
        String answer = "Hello from MQ: " + msg;

        RestAssured.given()
                .body(msg)
                .post("/jt400/messageQueue/write")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo(answer));

        LOGGER.debug("testMessageQueue: message '" + answer + "' was written. ");
        //register to delete
        getClientHelper().registerForRemoval(Jt400TestResource.RESOURCE_TYPE.messageQueue, answer);

        //read (the read message might be different in case the test runs in parallel

        RestAssured.post("/jt400/messageQueue/read")
                .then()
                .statusCode(200)
                //check of headers
                .body(Jt400Constants.SENDER_INFORMATION, Matchers.not(Matchers.empty()))
                .body(Jt400Constants.MESSAGE_FILE, Matchers.is(""))
                .body(Jt400Constants.MESSAGE_SEVERITY, Matchers.is(0))
                .body(Jt400Constants.MESSAGE_ID, Matchers.is(""))
                .body(Jt400Constants.MESSAGE_TYPE, Matchers.is(4))
                .body(Jt400Constants.MESSAGE, Matchers.startsWith("QueuedMessage: Hello "))
                .body("result", Matchers.equalTo(answer));
        //Jt400Constants.MESSAGE_DFT_RPY && Jt400Constants.MESSAGE_REPLYTO_KEY are used only for a special
        // type of message which can not be created by the camel component (*INQUIRY)
    }

    @Test
    public void testInquiryMessageQueue() throws Exception {
        LOGGER.debug("**** testInquiryMessageQueue() **: has started ");

        String msg = RandomStringUtils.secure().nextAlphanumeric(10).toLowerCase(Locale.ROOT);
        String replyMsg = "reply to: " + msg;
        getClientHelper().registerForRemoval(Jt400TestResource.RESOURCE_TYPE.replyToQueueu, msg);
        getClientHelper().registerForRemoval(Jt400TestResource.RESOURCE_TYPE.replyToQueueu, replyMsg);

        //sending a message using the same client as component
        getClientHelper().sendInquiry(msg);

        LOGGER.debug("testInquiryMessageQueue: message " + msg + " written via client");
        //register deletion of the message in case some following task fails
        QueuedMessage queuedMessage = getClientHelper().peekReplyToQueueMessage(msg);
        if (queuedMessage != null) {
            LOGGER.debug("testInquiryMessageQueue: message confirmed by peek: " + msg);
        }

        //set filter for expected messages (for parallel executions)
        RestAssured.given()
                .body(msg)
                .post("/jt400/inquiryMessageSetExpected")
                .then()
                .statusCode(204);

        //start route before sending message (and wait for start)
        Awaitility.await().atMost(WAIT_IN_SECONDS, TimeUnit.SECONDS).until(
                () -> RestAssured.get("/jt400/route/start/inquiryRoute")
                        .then()
                        .statusCode(200)
                        .extract().asString(),
                Matchers.is(Boolean.TRUE.toString()));
        LOGGER.debug("testInquiryMessageQueue: inquiry route started");

        //await to be processed
        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(WAIT_IN_SECONDS, TimeUnit.SECONDS).until(
                () -> RestAssured.get("/jt400/inquiryMessageProcessed")
                        .then()
                        .statusCode(200)
                        .extract().asString(),
                Matchers.is(String.valueOf(Boolean.TRUE)));
        LOGGER.debug("testInquiryMessageQueue: inquiry message processed");

        //stop route (and wait for stop)
        Awaitility.await().atMost(2 * WAIT_IN_SECONDS, TimeUnit.SECONDS).until(
                () -> RestAssured.get("/jt400/route/stop/inquiryRoute")
                        .then()
                        .statusCode(200)
                        .extract().asString(),
                Matchers.is(Boolean.TRUE.toString()));
        LOGGER.debug("testInquiryMessageQueue: inquiry route stooped");

        //check written message with client
        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(WAIT_IN_SECONDS, TimeUnit.SECONDS).until(
                () -> getClientHelper().peekReplyToQueueMessage(replyMsg),
                Matchers.notNullValue());
        LOGGER.debug("testInquiryMessageQueue: reply message confirmed by peek: " + replyMsg);
    }

    /**
     * Test for existence of resource bundle.
     * If the bundle is not loaded properly, the close of connection pool would fail
     * (see mor information in the resource method)
     *
     * @throws Exception
     */
    @Test
    public void testMissingResourceBundle() throws Exception {
        //stop component and then stop connectionPool, which is already stoppingm therefore requires resourceBundle to show the failure reason
        RestAssured.get("/jt400/component/stopWrong")
                .then()
                .statusCode(200);
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

    private static Jt400ClientHelper getClientHelper() {
        return Jt400TestResource.CLIENT_HELPER;
    }

}
