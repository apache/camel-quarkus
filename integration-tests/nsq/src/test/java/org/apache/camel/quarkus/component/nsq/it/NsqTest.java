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
package org.apache.camel.quarkus.component.nsq.it;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.brainlag.nsq.NSQConsumer;
import com.github.brainlag.nsq.NSQProducer;
import com.github.brainlag.nsq.exceptions.NSQException;
import com.github.brainlag.nsq.lookup.DefaultNSQLookup;
import com.github.brainlag.nsq.lookup.NSQLookup;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeAll;

import static io.restassured.RestAssured.given;
import static org.apache.camel.quarkus.component.nsq.it.NsqLogger.log;
import static org.apache.camel.quarkus.component.nsq.it.NsqRoute.CONSUMER_HOST_CFG_KEY;
import static org.apache.camel.quarkus.component.nsq.it.NsqRoute.CONSUMER_PORT_CFG_KEY;
import static org.apache.camel.quarkus.component.nsq.it.NsqRoute.CONSUMER_TOPIC;
import static org.apache.camel.quarkus.component.nsq.it.NsqRoute.MESSAGE_CHARSET;
import static org.apache.camel.quarkus.component.nsq.it.NsqRoute.PRODUCER_HOST_CFG_KEY;
import static org.apache.camel.quarkus.component.nsq.it.NsqRoute.PRODUCER_PORT_CFG_KEY;
import static org.apache.camel.quarkus.component.nsq.it.NsqRoute.PRODUCER_TOPIC;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@QuarkusTestResource(NsqTestResource.class)
class NsqTest {

    private static final Logger LOG = Logger.getLogger(NsqRoute.class);

    private static final String TEST_CONSUMER_MSG = "Hello NSQConsumer !";
    private static final String TEST_PRODUCER_MSG = "Hello NSQProducer !";
    private static final String TEST_REQUEUE_MSG = "Test Requeue";

    private static String CONSUMER_HOST, PRODUCER_HOST;
    private static int CONDUMER_PORT, PRODUCER_PORT;

    @BeforeAll
    public static void setUp() {
        CONSUMER_HOST = ConfigProvider.getConfig().getValue(CONSUMER_HOST_CFG_KEY, String.class);
        CONDUMER_PORT = ConfigProvider.getConfig().getValue(CONSUMER_PORT_CFG_KEY, int.class);
        PRODUCER_HOST = ConfigProvider.getConfig().getValue(PRODUCER_HOST_CFG_KEY, String.class);
        PRODUCER_PORT = ConfigProvider.getConfig().getValue(PRODUCER_PORT_CFG_KEY, int.class);

        log(LOG, "NsqTest.CONSUMER = %s:%s", CONSUMER_HOST, CONDUMER_PORT);
        log(LOG, "NsqTest.PRODUCER = %s:%s", PRODUCER_HOST, PRODUCER_PORT);
    }

    //@Test
    void nsqProducerShouldSucceed() throws Exception {

        CountDownLatch lock = new CountDownLatch(1);

        given().body(TEST_PRODUCER_MSG).post("/nsq/send").then().statusCode(204);

        AtomicInteger counter = new AtomicInteger(0);
        NSQLookup lookup = new DefaultNSQLookup();
        lookup.addLookupAddress(CONSUMER_HOST, CONDUMER_PORT);

        try (NSQConsumer consumer = new NSQConsumer(lookup, PRODUCER_TOPIC, "testconsumer", message -> {
            log(LOG, "The NSQConsumer from testProducer() received message %s", message);

            counter.incrementAndGet();
            message.finished();
            lock.countDown();

            assertEquals(TEST_PRODUCER_MSG, new String(message.getMessage(), MESSAGE_CHARSET));
        })) {
            consumer.start();

            lock.await(10, TimeUnit.SECONDS);

            assertEquals(1, counter.get());
        }
    }

    //@Test
    void nsqConsumerShouldSucceed() throws NSQException, TimeoutException {
        NSQProducer producer = new NSQProducer();
        producer.addAddress(PRODUCER_HOST, PRODUCER_PORT);
        producer.start();

        producer.produce(CONSUMER_TOPIC, TEST_CONSUMER_MSG.getBytes(MESSAGE_CHARSET));

        await().atMost(10L, TimeUnit.SECONDS).until(() -> {
            return given().get("/nsq/get-messages/testConsumer").statusCode() == 200;
        });
        given().get("/nsq/get-messages/testConsumer").then().body(is(TEST_CONSUMER_MSG));
    }

    //@Test
    void nsqConsumerWithExceptionShouldRequeueMessagesThreeTimes() throws NSQException, TimeoutException {
        NSQProducer producer = new NSQProducer();
        producer.addAddress(PRODUCER_HOST, PRODUCER_PORT);
        producer.start();

        producer.produce(CONSUMER_TOPIC, TEST_REQUEUE_MSG.getBytes(MESSAGE_CHARSET));

        await().atMost(10L, TimeUnit.SECONDS).until(() -> {
            return given().get("/nsq/get-messages/testRequeue").statusCode() == 200;
        });
        given().get("/nsq/get-messages/testRequeue").then().body(is(TEST_REQUEUE_MSG));
    }

}
