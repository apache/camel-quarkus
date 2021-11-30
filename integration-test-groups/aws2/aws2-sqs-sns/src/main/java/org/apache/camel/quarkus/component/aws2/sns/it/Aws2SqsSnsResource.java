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
package org.apache.camel.quarkus.component.aws2.sns.it;

import java.net.URI;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.aws2.sqs.Sqs2Constants;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/aws2-sqs-sns")
@ApplicationScoped
public class Aws2SqsSnsResource {

    @ConfigProperty(name = "aws-sqs.queue-name")
    String queueName;

    @ConfigProperty(name = "aws-sqs.sns-receiver-queue-name")
    String snsReceiverQueueName;

    @ConfigProperty(name = "aws2-sqs.sns-receiver-queue-arn")
    String snsReceiverQueueArn;

    @ConfigProperty(name = "aws-sns.topic-name")
    String topicName;

    @ConfigProperty(name = "aws-sqs.sns-fifo-receiver-queue-name")
    String snsFifoReceiverQueueName;

    @ConfigProperty(name = "aws2-sqs.sns-fifo-receiver-queue-arn")
    String snsFifoReceiverQueueArn;

    @ConfigProperty(name = "aws-sns-fifo.topic-name")
    String fifoTopicName;

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Path("/sqs/purge/queue/{queueName}")
    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public Response purgeQueue(@PathParam("queueName") String queueName) throws Exception {
        producerTemplate.sendBodyAndHeader(componentUri(queueName) + "?operation=purgeQueue",
                null,
                Sqs2Constants.SQS_QUEUE_PREFIX,
                queueName);
        return Response.ok().build();
    }

    @Path("/sns/send")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response snsSend(String message,
            @QueryParam("queueUrl") String queueUrl,
            @DefaultValue("false") @QueryParam("fifo") boolean fifo) throws Exception {

        final String response = producerTemplate.requestBody(
                String.format("aws2-sns://%s?subscribeSNStoSQS=true&queueUrl=RAW(%s)%s",
                        fifo ? fifoTopicName : topicName, fifo ? snsFifoReceiverQueueArn : snsReceiverQueueArn,
                        fifo ? "&messageGroupIdStrategy=useExchangeId" : ""),
                message,
                String.class);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response)
                .build();
    }

    @Path("/sqs/receive/{queueName}/{deleteMessage}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String sqsReceive(@PathParam("queueName") String queueName, @PathParam("deleteMessage") String deleteMessage)
            throws Exception {
        return consumerTemplate.receiveBody(componentUri(queueName)
                + "?deleteAfterRead=" + deleteMessage + "&deleteIfFiltered=" + deleteMessage + "&defaultVisibilityTimeout=0",
                10000,
                String.class);
    }

    @Path("/sns/receiveViaSqs")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String sqsReceiveViaSqs() throws Exception {
        return consumerTemplate.receiveBody("aws2-sqs://" + snsReceiverQueueName, 10000, String.class);
    }

    @Path("/snsFifo/receiveViaSqs")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String fifoSqsReceiveViaSqs() throws Exception {
        return consumerTemplate.receiveBody("aws2-sqs://" + snsFifoReceiverQueueName, 10000, String.class);
    }

    private String componentUri(String queueName) {
        return "aws2-sqs://" + queueName;
    }
}
