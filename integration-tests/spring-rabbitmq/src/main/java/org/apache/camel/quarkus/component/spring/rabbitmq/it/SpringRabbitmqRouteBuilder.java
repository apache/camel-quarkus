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

import java.util.concurrent.TimeUnit;

import com.rabbitmq.client.Channel;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.springrabbit.SpringRabbitMQConstants;

@ApplicationScoped
public class SpringRabbitmqRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        //Reuse endpoint and send to different destinations computed at runtime
        from("spring-rabbitmq:exchange-for-reuse1?queues=queue-for-reuse&routingKey=key-for-reuse1&connectionFactory=#connectionFactory&autoDeclare=true")
                .transform(body().prepend("Hello from reuse1 for key1: "))
                .to("direct:reuse");

        from("spring-rabbitmq:exchange-for-reuse2?queues=queue-for-reuse2&routingKey=key-for-reuse1&connectionFactory=#connectionFactory&autoDeclare=true")
                .transform(body().prepend("Hello from reuse2 for key1: "))
                .to("direct:reuse");

        from("spring-rabbitmq:exchange-for-reuse2?queues=queue-for-reuse2&routingKey=key-for-reuse2&connectionFactory=#connectionFactory&autoDeclare=true")
                .transform(body().prepend("Hello from reuse2 for key2: "))
                .to("direct:reuse");

        //fanout
        from("spring-rabbitmq:exchange-for-fanout?queues=queue-for-fanout-A&connectionFactory=#connectionFactory&autoDeclare=true&exchangeType=fanout")
                .transform(body().prepend("Hello from fanout for keyA: "))
                .to("direct:fanout-A");

        from("spring-rabbitmq:exchange-for-fanout?queues=queue-for-fanout-B&connectionFactory=#connectionFactory&autoDeclare=true&exchangeType=fanout")
                .transform(body().prepend("Hello from fanout for keyB: "))
                .to("direct:fanout-B");

        //topic
        from("spring-rabbitmq:exchange-for-topic?queues=queue-for-topicA&routingKey=topic.#&connectionFactory=#connectionFactory&autoDeclare=true&exchangeType=topic")
                .transform(body().prepend("Hello from topic: "))
                .to("direct:topic");

        //manual acknowledgement
        from("spring-rabbitmq:exchange-for-manual-ack?queues=queue-for-manual-ack&routingKey=key-for-manual-ack&connectionFactory=#connectionFactory&autoDeclare=true&acknowledgeMode=MANUAL")
                .process(exchange -> {
                    //simulate processing time 20 s (has to be bigger than timeout to read from direct routes
                    // -> see SpringRabbitmqResource.getFromDirect (5 seconds)
                    TimeUnit.SECONDS.sleep(20);
                    exchange.getIn().setBody("Processed: " + exchange.getIn().getBody(String.class));
                    Channel channel = exchange.getProperty(SpringRabbitMQConstants.CHANNEL, Channel.class);
                    long deliveryTag = exchange.getMessage().getHeader(SpringRabbitMQConstants.DELIVERY_TAG, long.class);
                    channel.basicAck(deliveryTag, true);
                })
                .to("direct:manual-ack");

        //deadletter
        from("spring-rabbitmq:exchange-for-deadletter?queues=queue-for-deadletter&routingKey=routing-key-for-deadletter&connectionFactory=#connectionFactory&autoDeclare=true&deadLetterExchange=exchange-for-deadletter-DL&deadLetterExchangeType=fanout&deadLetterQueue=exchange-for-deadletter-DL&deadLetterRoutingKey=any-key&rejectAndDontRequeue=true")
                .process(exchange -> {
                    //forced exception
                    throw new RuntimeException("forced exception to trigger dead letter exchange");
                })
                .to("direct:deadletter");

        //redirection from deadletter queue to direct (with autoDeclare = false,because the binding is created by above route)
        from("spring-rabbitmq:exchange-for-deadletter-DL?queues=queue-for-deadletter-DL&connectionFactory=#connectionFactory&exchangeType=fanout")
                .transform(body().prepend("Hello from deadletter: "))
                .to("direct:deadletter-DL");

        //dmlc
        from("spring-rabbitmq:exchange-for-dmlc?queues=queue-for-dmlc&routingKey=key-for-dmlc&connectionFactory=#connectionFactory&autoDeclare=true&messageListenerContainerType=DMLC&concurrentConsumers=5")
                .process(exchange -> {
                    //delay 1 second
                    TimeUnit.SECONDS.sleep(1);
                })
                .transform(body().prepend("Hello from DMLC: "))
                .to("direct:dmlc");

        //smlc
        from("spring-rabbitmq:exchange-for-smlc?queues=queueu-for-smlc&routingKey=key-for-smlc&connectionFactory=#connectionFactory&autoDeclare=true&messageListenerContainerType=SMLC&maxConcurrentConsumers=1")
                .process(exchange -> {
                    //delay 1 second
                    TimeUnit.SECONDS.sleep(1);
                })
                .transform(body().prepend("Hello from SMLC: "))
                .to("direct:smlc");
    }
}
