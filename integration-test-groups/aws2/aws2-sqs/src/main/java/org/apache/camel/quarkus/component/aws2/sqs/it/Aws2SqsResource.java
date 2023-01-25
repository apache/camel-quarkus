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
package org.apache.camel.quarkus.component.aws2.sqs.it;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.aws2.sqs.Sqs2Constants;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.services.sqs.model.ListQueuesResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;

@Path("/aws2-sqs")
@ApplicationScoped
public class Aws2SqsResource {

    @ConfigProperty(name = "aws-sqs.queue-name")
    String queueName;

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Path("send")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response sqsSend(String message) throws Exception {
        final String response = producerTemplate.requestBody(componentUri(), message, String.class);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response)
                .build();
    }

    @Path("send/{queueName}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response sqsSendToSpecificQueue(@PathParam("queueName") String queueName, String message) throws Exception {
        final String response = producerTemplate.requestBody(componentUri(queueName), message, String.class);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response)
                .build();
    }

    @Path("purge/queue/{queueName}")
    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public Response purgeQueue(@PathParam("queueName") String queueName) throws Exception {
        producerTemplate.sendBody(componentUri(queueName) + "?operation=purgeQueue",
                null);
        return Response.ok().build();
    }

    @Path("receive/{queueName}/{deleteMessage}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String sqsReceive(@PathParam("queueName") String queueName, @PathParam("deleteMessage") String deleteMessage)
            throws Exception {
        return consumerTemplate.receiveBody(componentUri(queueName)
                + "?deleteAfterRead=" + deleteMessage + "&deleteIfFiltered=" + deleteMessage + "&defaultVisibilityTimeout=0",
                10000,
                String.class);
    }

    @Path("receive/receipt/{queueName}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String sqsReceipt(@PathParam("queueName") String queueName) throws Exception {
        return consumerTemplate.receive(componentUri(queueName), 10000)
                .getIn()
                .getHeader(Sqs2Constants.RECEIPT_HANDLE)
                .toString();
    }

    @Path("queues")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> listQueues() throws Exception {
        return producerTemplate.requestBody(componentUri() + "?operation=listQueues", null, ListQueuesResponse.class)
                .queueUrls();
    }

    @Path("batch")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response sendBatchMessage(List<String> messages) throws Exception {
        final SendMessageBatchResponse response = producerTemplate.requestBody(
                componentUri() + "?operation=sendBatchMessage",
                messages,
                SendMessageBatchResponse.class);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity("" + response.successful().size())
                .build();
    }

    @Path("delete/message/{queueName}/{receipt}")
    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public Response deleteMessage(@PathParam("queueName") String queueName, @PathParam("receipt") String receipt)
            throws Exception {
        producerTemplate.sendBodyAndHeader(componentUri(queueName) + "?operation=deleteMessage",
                null,
                Sqs2Constants.RECEIPT_HANDLE,
                URLDecoder.decode(receipt, StandardCharsets.UTF_8));
        return Response.ok().build();
    }

    @Path("delete/queue/{queueName}")
    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public Response deleteQueue(@PathParam("queueName") String queueName) throws Exception {
        producerTemplate.sendBody(componentUri(queueName) + "?operation=deleteQueue",
                null);
        return Response.ok().build();
    }

    @Path("queue/autocreate/delayed/{queueName}/{delay}/{msg}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String autoCreateDelayedQueue(@PathParam("queueName") String queueName, @PathParam("delay") String delay,
            @PathParam("msg") String msg) {
        String uri = String.format("aws2-sqs://%s?autoCreateQueue=true&delayQueue=true&delaySeconds=%s",
                queueName, delay);
        return producerTemplate
                .requestBody(
                        uri,
                        msg,
                        String.class);
    }

    private String componentUri() {
        return "aws2-sqs://" + queueName;
    }

    private String componentUri(String queueName) {
        return "aws2-sqs://" + queueName;
    }

}
