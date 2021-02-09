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
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.services.sqs.model.ListQueuesResponse;

@Path("/aws2-sqs-sns")
@ApplicationScoped
public class Aws2SqsResource {

    @ConfigProperty(name = "aws-sqs.queue-name")
    String queueName;

    @ConfigProperty(name = "aws-sqs.sns-receiver-queue-name")
    String snsReceiverQueueName;

    @ConfigProperty(name = "aws2-sqs.sns-receiver-queue-arn")
    String snsReceiverQueueArn;

    @ConfigProperty(name = "aws-sns.topic-name")
    String topicName;

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Path("/sqs/send")
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

    @Path("/sqs/receive")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String sqsReceive() throws Exception {
        return consumerTemplate.receiveBody(componentUri(), 10000, String.class);
    }

    @Path("/sqs/queues")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> listQueues() throws Exception {
        return producerTemplate.requestBody(componentUri() + "?operation=listQueues", null, ListQueuesResponse.class)
                .queueUrls();
    }

    private String componentUri() {
        return "aws2-sqs://" + queueName;
    }

    @Path("/sns/send")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response snsSend(String message, @QueryParam("queueUrl") String queueUrl) throws Exception {

        final String response = producerTemplate.requestBody(
                "aws2-sns://" + topicName + "?subscribeSNStoSQS=true&queueUrl=RAW(" + snsReceiverQueueArn + ")", message,
                String.class);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response)
                .build();
    }

    @Path("/sns/receiveViaSqs")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String sqsReceiveViaSqs() throws Exception {
        return consumerTemplate.receiveBody("aws2-sqs://" + snsReceiverQueueName, 10000, String.class);
    }

}
