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

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.apache.camel.component.springrabbit.SpringRabbitMQConstants;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;

import static org.hamcrest.Matchers.is;

@QuarkusTest
@QuarkusTestResource(SpringRabbitmqTestResource.class)
class SpringRabbitmqTest {

    private final static String EXCHANGE_POLLING = "polling";
    private final static String ROUTING_KEY_POLLING = "pollingKey";
    private ConnectionFactory connectionFactory;

    //@Test
    public void testInOutDMLC() {
        testInOut(SpringRabbitMQConstants.DIRECT_MESSAGE_LISTENER_CONTAINER);
    }

    //@Test
    public void testInOutSMLC() {
        testInOut(SpringRabbitMQConstants.SIMPLE_MESSAGE_LISTENER_CONTAINER);
    }

    public void testInOut(String type) {
        sendToExchange(SpringRabbitmqResource.EXCHANGE_IN_OUT + type,
                SpringRabbitmqResource.ROUTING_KEY_IN_OUT + type, "Sheldon");

        getFromDirect(SpringRabbitmqResource.DIRECT_IN_OUT + type)
                .then()
                .statusCode(200)
                .body(is("Hello Sheldon"));
    }

    //@Test
    public void testPolling() throws InterruptedException {

        bindQueue(SpringRabbitmqResource.POLLING_QUEUE_NAME, EXCHANGE_POLLING, ROUTING_KEY_POLLING);

        //start thread with the poling consumer from exchange "polling", polling queue, routing "pollingKey", result is sent to polling direct
        RestAssured.given()
                .queryParam(SpringRabbitmqResource.QUERY_EXCHANGE, EXCHANGE_POLLING)
                .queryParam(SpringRabbitmqResource.QUERY_ROUTING_KEY, ROUTING_KEY_POLLING)
                .post("/spring-rabbitmq/startPolling");

        // wait a little to demonstrate we can start poll before we have a msg on the queue
        Thread.sleep(500);

        sendToExchange(EXCHANGE_POLLING, ROUTING_KEY_POLLING, "Sheldon");

        //get result from direct (for pooling) with timeout
        getFromDirect(SpringRabbitmqResource.DIRECT_POLLING)
                .then()
                .statusCode(200)
                .body(is("Polling Hello Sheldon"));

    }

    private void sendToExchange(String exchange, String routingKey, String body) {
        RestAssured.given()
                .queryParam(SpringRabbitmqResource.QUERY_EXCHANGE, exchange)
                .queryParam(SpringRabbitmqResource.QUERY_ROUTING_KEY, routingKey)
                .body(body)
                .post("/spring-rabbitmq/send");
    }

    private Response getFromDirect(String direct) {
        return RestAssured.given()
                .queryParam(SpringRabbitmqResource.QUERY_DIRECT, direct)
                .post("/spring-rabbitmq/getFromDirect");
    }

    private void bindQueue(String queue, String exchange, String routingKey) {
        Queue q = new Queue(queue, false);
        DirectExchange t = new DirectExchange(exchange);
        AmqpAdmin admin = new RabbitAdmin(connectionFactory);
        admin.declareQueue(q);
        admin.declareExchange(t);
        admin.declareBinding(BindingBuilder.bind(q).to(t).with(routingKey));
    }

    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }
}
