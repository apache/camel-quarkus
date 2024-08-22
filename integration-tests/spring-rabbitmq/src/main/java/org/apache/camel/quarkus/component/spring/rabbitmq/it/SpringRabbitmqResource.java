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

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.concurrent.Executors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.jboss.logging.Logger;

@Path("/spring-rabbitmq")
@ApplicationScoped
public class SpringRabbitmqResource {
    private static final Logger LOG = Logger.getLogger(SpringRabbitmqResource.class);

    public static final String PARAMETER_PORT = "camel.quarkus.spring-rabbitmq.test.port";
    public static final String PARAMETER_HOSTNAME = "camel.quarkus.spring-rabbitmq.test.hostname";
    public static final String PARAMETER_USERNAME = "camel.quarkus.spring-rabbitmq.test.username";
    public static final String PARAMETER_PASSWORD = "camel.quarkus.spring-rabbitmq.test.password";

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Path("/consume")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String consume(@QueryParam("queue") String queue,
            @QueryParam("exchange") String exchange,
            @QueryParam("routingKey") String routingKey) {
        String url = "spring-rabbitmq:" + exchange + "?connectionFactory=#connectionFactory";
        if (routingKey != null) {
            url += "&routingKey=" + routingKey;
        }
        if (queue != null) {
            url += "&queues=" + queue;
        }
        return consumerTemplate.receiveBody(url, 5000, String.class);
    }

    @Path("/getFromDirect")
    @POST
    public Response getFromDirect(String directName,
            @QueryParam("timeout") Long timeout,
            @QueryParam("duration") boolean duration,
            @QueryParam("numberOfMessages") Integer numberOfMessages) {
        long _timeout = timeout != null ? timeout : 5000;
        if (numberOfMessages != null) {
            Instant start = Instant.now();
            LinkedList<String> results = new LinkedList<>();
            for (int i = 0; i < numberOfMessages; i++) {
                String msg = consumerTemplate.receiveBody(directName, _timeout, String.class);
                if (msg == null || msg.isEmpty()) {
                    break;
                }
                results.add(msg);
            }

            Duration timeElapsed = Duration.between(start, Instant.now());
            if (duration) {
                results.addFirst(timeElapsed.getSeconds() + "");
            }

            return Response.ok(SpringRabbitmqUtil.listToString(results)).build();
        }

        return Response.ok(consumerTemplate.receiveBody(directName, _timeout, String.class)).build();
    }

    @Path("/send")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public void send(String message,
            @QueryParam("headers") String headers,
            @QueryParam("exchange") String exchange,
            @QueryParam("routingKey") String routingKey,
            @QueryParam("exchangeType") String exchangeType,
            @QueryParam("componentName") String componentName) {
        String url = String.format(
                "%s:%s?connectionFactory=#connectionFactory",
                componentName == null ? "spring-rabbitmq" : componentName, exchange);

        if (routingKey != null) {
            url += "&routingKey=" + routingKey;
        }
        if (exchangeType != null) {
            url += "&exchangeType=" + exchangeType;
        }

        if (headers != null) {
            producerTemplate.sendBodyAndHeaders(url, message, SpringRabbitmqUtil.stringToMap(headers));
        } else {
            producerTemplate.sendBody(url, message);
        }
    }

    @Path("/startPolling")
    @POST
    public void startPolling(@QueryParam("exchange") String exchange,
            @QueryParam("routingKey") String routingKey) {
        // use another thread for polling consumer to demonstrate that we can wait before
        // the message is sent to the queue
        Executors.newSingleThreadExecutor().execute(() -> {
            String url = String.format("spring-rabbitmq:%s?queues=%s&routingKey=%s", exchange, "queue-for-polling", routingKey);
            String body = consumerTemplate.receiveBody(url, String.class);
            producerTemplate.sendBody("direct:polling", "Polling Hello " + body);
        });
    }
}
