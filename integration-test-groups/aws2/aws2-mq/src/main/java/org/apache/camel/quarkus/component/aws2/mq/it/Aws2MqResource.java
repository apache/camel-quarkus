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
package org.apache.camel.quarkus.component.aws2.mq.it;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.aws2.mq.MQ2Constants;
import org.apache.camel.component.aws2.mq.MQ2Operations;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.mq.MqClient;
import software.amazon.awssdk.services.mq.MqClientBuilder;
import software.amazon.awssdk.services.mq.model.BrokerSummary;
import software.amazon.awssdk.services.mq.model.ConfigurationId;
import software.amazon.awssdk.services.mq.model.CreateBrokerRequest;
import software.amazon.awssdk.services.mq.model.CreateBrokerResponse;
import software.amazon.awssdk.services.mq.model.DeleteBrokerRequest;
import software.amazon.awssdk.services.mq.model.DeploymentMode;
import software.amazon.awssdk.services.mq.model.DescribeBrokerRequest;
import software.amazon.awssdk.services.mq.model.DescribeBrokerResponse;
import software.amazon.awssdk.services.mq.model.EngineType;
import software.amazon.awssdk.services.mq.model.ListBrokersResponse;
import software.amazon.awssdk.services.mq.model.RebootBrokerRequest;
import software.amazon.awssdk.services.mq.model.UpdateBrokerRequest;
import software.amazon.awssdk.services.mq.model.User;

import static org.apache.camel.component.aws2.mq.MQ2Operations.*;

@Path("/aws2-mq")
@ApplicationScoped
public class Aws2MqResource {

    @Inject
    ProducerTemplate producerTemplate;

    @ConfigProperty(name = "aws.mq.broker.engine.version")
    String brokerEngineVersion;

    @ConfigProperty(name = "aws.mq.broker.admin.password")
    String brokerAdminPassword;

    @ConfigProperty(name = "camel.component.aws2-mq.access-key")
    String accessKey;

    @ConfigProperty(name = "camel.component.aws2-mq.secret-key")
    String secretKey;

    @ConfigProperty(name = "camel.component.aws2-mq.region")
    String awsRegion;

    @ConfigProperty(name = "camel.component.aws2-mq.uri-endpoint-override")
    Optional<String> uriEndpointOverride;

    @ConfigProperty(name = "camel.component.aws2-mq.override-endpoint")
    boolean overrideEndpoint;

    @Path("/brokers")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> listBrokers() {
        return producerTemplate.requestBody(
                componentUri(listBrokers),
                null,
                ListBrokersResponse.class)
                .brokerSummaries().stream()
                .map(BrokerSummary::brokerName)
                .collect(Collectors.toList());
    }

    @Path("/broker/{brokerName}")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response createBroker(@PathParam("brokerName") String brokerName) throws Exception {
        User adminUser = User.builder()
                .username("admin")
                .password(brokerAdminPassword)
                .build();

        CreateBrokerResponse response = producerTemplate.requestBodyAndHeaders(
                componentUri(createBroker),
                null,
                Map.of(
                        MQ2Constants.BROKER_NAME, brokerName,
                        MQ2Constants.BROKER_ENGINE, EngineType.ACTIVEMQ.toString(),
                        MQ2Constants.BROKER_ENGINE_VERSION, brokerEngineVersion,
                        MQ2Constants.BROKER_DEPLOYMENT_MODE, DeploymentMode.SINGLE_INSTANCE.toString(),
                        MQ2Constants.BROKER_INSTANCE_TYPE, "mq.t3.micro",
                        MQ2Constants.BROKER_USERS, List.of(adminUser),
                        MQ2Constants.BROKER_PUBLICLY_ACCESSIBLE, false),
                CreateBrokerResponse.class);

        return Response.created(new URI("https://camel.apache.org/")).entity(response.brokerId()).build();
    }

    @Path("/broker/{brokerName}/pojo")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response createBrokerPojo(@PathParam("brokerName") String brokerName) throws Exception {
        User adminUser = User.builder()
                .username("admin")
                .password(brokerAdminPassword)
                .build();

        CreateBrokerRequest request = CreateBrokerRequest.builder()
                .brokerName(brokerName)
                .engineType(EngineType.ACTIVEMQ)
                .engineVersion(brokerEngineVersion)
                .deploymentMode(DeploymentMode.SINGLE_INSTANCE)
                .hostInstanceType("mq.t3.micro")
                .users(adminUser)
                .publiclyAccessible(false)
                .build();

        CreateBrokerResponse response = producerTemplate.requestBody(
                componentUri(createBroker) + "&pojoRequest=true",
                request,
                CreateBrokerResponse.class);

        return Response.created(new URI("https://camel.apache.org/")).entity(response.brokerId()).build();
    }

    @Path("/broker/describe")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String describeBroker(@QueryParam("brokerId") String brokerId) {
        return producerTemplate.requestBodyAndHeader(
                componentUri(describeBroker),
                null,
                MQ2Constants.BROKER_ID,
                brokerId,
                DescribeBrokerResponse.class)
                .brokerStateAsString();
    }

    @Path("/broker/configurationId")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getBrokerConfigurationId(@QueryParam("brokerId") String brokerId) {
        MqClientBuilder builder = MqClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)));
        if (overrideEndpoint && uriEndpointOverride.isPresent()) {
            builder.endpointOverride(URI.create(uriEndpointOverride.get()));
        }
        try (MqClient mqClient = builder.build()) {
            return mqClient.describeBroker(DescribeBrokerRequest.builder()
                    .brokerId(brokerId)
                    .build())
                    .configurations().current().id();
        }
    }

    @Path("/broker/describe/pojo")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String describeBrokerPojo(@QueryParam("brokerId") String brokerId) {
        DescribeBrokerRequest request = DescribeBrokerRequest.builder()
                .brokerId(brokerId)
                .build();
        return producerTemplate.requestBody(
                componentUri(describeBroker) + "&pojoRequest=true",
                request,
                DescribeBrokerResponse.class)
                .brokerStateAsString();
    }

    @Path("/broker")
    @PUT
    public Response updateBroker(@QueryParam("brokerId") String brokerId,
            @QueryParam("configurationId") String configurationId) {
        ConfigurationId configuration = ConfigurationId.builder()
                .id(configurationId)
                .build();
        producerTemplate.requestBodyAndHeaders(
                componentUri(updateBroker),
                null,
                Map.of(
                        MQ2Constants.BROKER_ID, brokerId,
                        MQ2Constants.CONFIGURATION_ID, configuration),
                Object.class);
        return Response.ok().build();
    }

    @Path("/broker/pojo")
    @PUT
    public Response updateBrokerPojo(@QueryParam("brokerId") String brokerId) {
        UpdateBrokerRequest request = UpdateBrokerRequest.builder()
                .brokerId(brokerId)
                .autoMinorVersionUpgrade(true)
                .build();
        producerTemplate.requestBody(
                componentUri(updateBroker) + "&pojoRequest=true",
                request,
                Object.class);
        return Response.ok().build();
    }

    @Path("/broker/{brokerId}/reboot")
    @POST
    public void rebootBroker(@PathParam("brokerId") String brokerId) {
        producerTemplate.requestBodyAndHeader(
                componentUri(rebootBroker),
                null,
                MQ2Constants.BROKER_ID,
                brokerId);
    }

    @Path("/broker/{brokerId}/reboot/pojo")
    @POST
    public void rebootBrokerPojo(@PathParam("brokerId") String brokerId) {
        RebootBrokerRequest request = RebootBrokerRequest.builder()
                .brokerId(brokerId)
                .build();
        producerTemplate.requestBody(
                componentUri(rebootBroker) + "&pojoRequest=true",
                request,
                Object.class);
    }

    @Path("/broker")
    @DELETE
    public void deleteBroker(@QueryParam("brokerId") String brokerId) {
        producerTemplate.requestBodyAndHeader(
                componentUri(deleteBroker),
                null,
                MQ2Constants.BROKER_ID,
                brokerId);
    }

    @Path("/broker/pojo")
    @DELETE
    public void deleteBrokerPojo(@QueryParam("brokerId") String brokerId) {
        DeleteBrokerRequest request = DeleteBrokerRequest.builder()
                .brokerId(brokerId)
                .build();
        producerTemplate.requestBody(
                componentUri(deleteBroker) + "&pojoRequest=true",
                request,
                Object.class);
    }

    private String componentUri(MQ2Operations operation) {
        return "aws2-mq:mq?operation=" + operation;
    }
}
