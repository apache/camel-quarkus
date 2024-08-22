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
package org.apache.camel.quarkus.component.spring.rabbitmq.it;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.camel.component.springrabbit.SpringRabbitMQConstants;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@WithTestResource(SpringRabbitmqTestResource.class)
class SpringRabbitmqTest {

    private final static String EXCHANGE_POLLING = "polling";
    private final static String ROUTING_KEY_POLLING = "pollingKey";
    private ConnectionFactory connectionFactory;

    @Test
    public void testDefaultExchangeName() {
        //autodeclare does not work for producers, therefore the queue has to be prepared in advance
        bindQueue("queue-for-default", "any_exchange", "any_key");
        //send a message using default keyword, so the routingKey will be used as queue
        sendToExchange("default", "queue-for-default", "content for default test");

        //read from "queueForDefault" using default exchange name without routingKey
        RestAssured.given()
                .queryParam("exchange", "default")
                .queryParam("queue", "queue-for-default")
                .post("/spring-rabbitmq/consume")
                .then()
                .statusCode(200)
                .body(is("content for default test"));
    }

    @Test
    public void testHeadersToProperties() throws UnsupportedEncodingException {
        //autodeclare does not work for producers, therefore the queue has to be prepared in advance
        bindQueue("queue-for-headersToProperties", "exchange-for-headersToProperties", "key-for-headersToProperties");

        String headers = SpringRabbitmqUtil
                .mapToString(Map.of(SpringRabbitMQConstants.DELIVERY_MODE, MessageDeliveryMode.PERSISTENT,
                        SpringRabbitMQConstants.TYPE, "price",
                        SpringRabbitMQConstants.CONTENT_TYPE, "application/xml",
                        SpringRabbitMQConstants.MESSAGE_ID, "0fe9c142-f9c1-426f-9237-f5a4c988a8ae",
                        SpringRabbitMQConstants.PRIORITY, 1));

        RestAssured.given()
                .queryParam("exchange", "exchange-for-headersToProperties")
                .queryParam("routingKey", "key-for-headersToProperties")
                .queryParam("headers", headers)
                .queryParam("componentName", "customHeaderFilterStrategySpringRabbitComponent")
                .body("<price>123</price>")
                .post("/spring-rabbitmq/send").then().statusCode(204);

        AmqpTemplate template = new RabbitTemplate(connectionFactory);
        Message out = template.receive("queue-for-headersToProperties");

        final MessageProperties messageProperties = out.getMessageProperties();
        Assertions.assertNotNull(messageProperties, "The message properties should not be null");
        String encoding = messageProperties.getContentEncoding();
        assertThat(Charset.defaultCharset().name()).isEqualTo(encoding);
        assertThat(new String(out.getBody(), encoding)).isEqualTo("<price>123</price>");
        assertThat(messageProperties.getReceivedDeliveryMode()).isEqualTo(MessageDeliveryMode.PERSISTENT);
        assertThat(messageProperties.getType()).isEqualTo("price");
        assertThat(messageProperties.getContentType()).isEqualTo("application/xml");
        assertThat(messageProperties.getMessageId()).isEqualTo("0fe9c142-f9c1-426f-9237-f5a4c988a8ae");
        assertThat(messageProperties.getPriority()).isEqualTo(1);
        //the only headers preserved by customHeadersFilterStrategy is "CamelSpringRabbitmqMessageId
        assertThat(messageProperties.getHeaders().size()).isEqualTo(1);
        assertThat(messageProperties.getHeaders()).containsKey("CamelSpringRabbitmqMessageId");
    }

    @Test
    public void testReuse() {
        //send msg without reuse
        RestAssured.given()
                .queryParam("exchange", "exchange-for-reuse1")
                .queryParam("routingKey", "key-for-reuse1")
                .body("Hello")
                .post("/spring-rabbitmq/send")
                .then()
                .statusCode(204);

        getFromDirect("direct:reuse")
                .then()
                .statusCode(200)
                .body(is("Hello from reuse1 for key1: Hello"));

        //overriding exchange
        RestAssured.given()
                .queryParam("exchange", "exchange-for-reuse1")
                .queryParam("routingKey", "key-for-reuse1")
                .queryParam("headers",
                        SpringRabbitmqUtil
                                .mapToString(Map.of(SpringRabbitMQConstants.EXCHANGE_OVERRIDE_NAME, "exchange-for-reuse2")))
                .body("Hello")
                .post("/spring-rabbitmq/send")
                .then()
                .statusCode(204);

        getFromDirect("direct:reuse")
                .then()
                .statusCode(200)
                .body(is("Hello from reuse2 for key1: Hello"));

        //overriding exchange and key
        RestAssured.given()
                .queryParam("exchange", "exchange-for-reuse1")
                .queryParam("routingKey", "key-for-reuse1")
                .queryParam("headers",
                        SpringRabbitmqUtil
                                .mapToString(Map.of(SpringRabbitMQConstants.EXCHANGE_OVERRIDE_NAME, "exchange-for-reuse2",
                                        SpringRabbitMQConstants.ROUTING_OVERRIDE_KEY, "key-for-reuse2")))
                .body("Hello")
                .post("/spring-rabbitmq/send")
                .then()
                .statusCode(204);

        getFromDirect("direct:reuse")
                .then()
                .statusCode(200)
                .body(is("Hello from reuse2 for key2: Hello"));
    }

    @Test
    public void testManualAcknowledgement() {
        //autodeclare does not work for producers, therefore the queue has to be prepared in advance
        bindQueue("queue-for-manual-ack", "exchange-for-manual-ack", "key-for-manual-ack");
        //send message with 20 seconds processing time
        RestAssured.given()
                .queryParam("exchange", "exchange-for-manual-ack")
                .queryParam("routingKey", "key-for-manual-ack")
                .body("Hello")
                .post("/spring-rabbitmq/send")
                .then()
                .statusCode(204);

        //message is not acked in rabbitmq (in 5 seconds)
        getFromDirect("direct:manual-ack")
                .then()
                .statusCode(200)
                .body(is(""));

        //should be acked in 20 seconds
        Awaitility.await().atMost(20, TimeUnit.SECONDS).untilAsserted(() -> {
            Response res = getFromDirect("direct:manual-ack");

            assertThat(res.statusCode()).isEqualTo(200);
            assertThat(res.body().asString()).isEqualTo("Processed: Hello");
        });
    }

    @Test
    public void testDMLC() {
        int count = 20;
        //sent 20 messages
        for (int i = 0; i < count; i++) {

            RestAssured.given()
                    .queryParam("exchange", "exchange-for-dmlc")
                    .queryParam("routingKey", "key-for-dmlc")
                    .body("Hello" + i)
                    .post("/spring-rabbitmq/send").then().statusCode(204);
        }

        //read results
        List<Object> results = SpringRabbitmqUtil.stringToList(RestAssured.given()
                .body("direct:dmlc")
                .queryParam("numberOfMessages", 20)
                .queryParam("duration", true)
                .queryParam("cacheResults", true)
                .post("/spring-rabbitmq/getFromDirect")
                .then().statusCode(200)
                .extract().body().asString());

        assertThat(results).hasSize(count + 1);
        //the whole duration has to be longer than count
        // (part of the first second is taken for the calls to resource, so the duration has to be >= count-1)
        assertThat(Integer.parseInt((String) results.get(0))).isLessThan(count - 1);
        for (int i = 0; i < count; i++) {
            assertThat(results).contains("Hello from DMLC: Hello" + i);
        }
    }

    @Test
    public void testSMLC() {
        int count = 20;
        //sent 20 messages
        for (int i = 0; i < count; i++) {

            RestAssured.given()
                    .queryParam("exchange", "exchange-for-smlc")
                    .queryParam("routingKey", "key-for-smlc")
                    .body("Hello" + i)
                    .post("/spring-rabbitmq/send").then().statusCode(204);
        }

        //read results
        List<Object> results = SpringRabbitmqUtil.stringToList(RestAssured.given()
                .body("direct:smlc")
                .queryParam("numberOfMessages", 20)
                .queryParam("duration", true)
                .queryParam("cacheResults", true)
                .post("/spring-rabbitmq/getFromDirect")
                .then().statusCode(200)
                .extract().body().asString());

        assertThat(results).hasSize(count + 1);
        //the whole duration has to be longer than count
        // (part of the first second is taken for the calls to resource, so the duration has to be >= count-1)
        assertThat(Integer.parseInt((String) results.get(0))).isGreaterThanOrEqualTo(count - 1);
        for (int i = 0; i < count; i++) {
            assertThat(results).contains("Hello from SMLC: Hello" + i);
        }
    }

    @Test
    public void testPolling() throws InterruptedException {

        bindQueue("queue-for-polling", "exchange-for-polling", "key-for-polling");

        //start thread with the poling consumer from exchange "polling", polling queue, routing "pollingKey", result is sent to polling direct
        RestAssured.given()
                .queryParam("exchange", "exchange-for-polling")
                .queryParam("routingKey", "key-for-polling")
                .post("/spring-rabbitmq/startPolling");

        // wait a little to demonstrate we can start poll before we have a msg on the queue
        Thread.sleep(500);

        sendToExchange("exchange-for-polling", "key-for-polling", "Sheldon");

        //get result from direct (for polling) with timeout
        getFromDirect("direct:polling")
                .then()
                .statusCode(200)
                .body(is("Polling Hello Sheldon"));

    }

    @Test
    public void testTypeFanout() {
        //send message without key to fanout exchange
        RestAssured.given()
                .queryParam("exchange", "exchange-for-fanout")
                .body("Hello")
                .post("/spring-rabbitmq/send")
                .then()
                .statusCode(204);

        getFromDirect("direct:fanout-A")
                .then()
                .statusCode(200)
                .body(is("Hello from fanout for keyA: Hello"));

        getFromDirect("direct:fanout-B")
                .then()
                .statusCode(200)
                .body(is("Hello from fanout for keyB: Hello"));
    }

    @Test
    public void testTypeTopic() {
        RestAssured.given()
                .queryParam("exchange", "exchange-for-topic")
                .queryParam("routingKey", "topic.1")
                .body("Hello1")
                .post("/spring-rabbitmq/send")
                .then()
                .statusCode(204);
        //different topic
        RestAssured.given()
                .queryParam("exchange", "exchange-for-topic")
                .queryParam("routingKey", "wrong_topic.2")
                .body("Hello2")
                .post("/spring-rabbitmq/send")
                .then()
                .statusCode(204);
        RestAssured.given()
                .queryParam("exchange", "exchange-for-topic")
                .queryParam("routingKey", "topic.3")
                .body("Hello3")
                .post("/spring-rabbitmq/send")
                .then()
                .statusCode(204);

        List<Object> results = SpringRabbitmqUtil.stringToList(RestAssured.given()
                .body("direct:topic")
                //catch 3 messages to be sure that hello2 is not there
                .queryParam("numberOfMessages", 3)
                .post("/spring-rabbitmq/getFromDirect")
                .then().statusCode(200)
                .extract().body().asString());
        assertThat(results).contains("Hello from topic: Hello1", "Hello from topic: Hello3");
        assertThat(results).doesNotContain("Hello from topic: Hello2");
    }

    @Test
    public void testDeadLetter() {
        RestAssured.given()
                .queryParam("exchange", "exchange-for-deadletter")
                .queryParam("routingKey", "routing-key-for-deadletter")
                .body("Hello")
                .post("/spring-rabbitmq/send")
                .then()
                .statusCode(204);

        //message ends in deadletter
        getFromDirect("direct:deadletter-DL")
                .then()
                .statusCode(200)
                .body(is("Hello from deadletter: Hello"));

        //no message ends in the "successful scenario" direct
        getFromDirect("direct:deadletter")
                .then()
                .statusCode(200)
                .body(is(""));

    }

    private void sendToExchange(String exchange, String routingKey, String body) {
        RequestSpecification rs = RestAssured.given()
                .queryParam("exchange", exchange)
                .queryParam("routingKey", routingKey)
                .body(body);

        rs.post("/spring-rabbitmq/send");
    }

    private Response getFromDirect(String direct) {
        return RestAssured.given()
                .body(direct)
                .post("/spring-rabbitmq/getFromDirect");
    }

    private void bindQueue(String queue, String exchange, String routingKey) {
        Queue q = new Queue(queue, false);

        AmqpAdmin admin = new RabbitAdmin(connectionFactory);
        admin.declareQueue(q);
        DirectExchange t = new DirectExchange(exchange);
        admin.declareExchange(t);
        admin.declareBinding(BindingBuilder.bind(q).to(t).with(routingKey));
    }

    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }
}
