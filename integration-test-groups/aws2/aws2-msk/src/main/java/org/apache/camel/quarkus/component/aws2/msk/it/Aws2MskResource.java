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
package org.apache.camel.quarkus.component.aws2.msk.it;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.aws2.msk.MSK2Constants;
import org.apache.camel.component.aws2.msk.MSK2Operations;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.services.kafka.model.BrokerNodeGroupInfo;
import software.amazon.awssdk.services.kafka.model.ClusterInfo;
import software.amazon.awssdk.services.kafka.model.CreateClusterRequest;
import software.amazon.awssdk.services.kafka.model.CreateClusterResponse;
import software.amazon.awssdk.services.kafka.model.DeleteClusterRequest;
import software.amazon.awssdk.services.kafka.model.DescribeClusterRequest;
import software.amazon.awssdk.services.kafka.model.DescribeClusterResponse;
import software.amazon.awssdk.services.kafka.model.ListClustersResponse;

import static org.apache.camel.component.aws2.msk.MSK2Operations.*;

@Path("/aws2-msk")
@ApplicationScoped
public class Aws2MskResource {

    @Inject
    ProducerTemplate producerTemplate;

    @ConfigProperty(name = "aws.msk.client.subnets")
    List<String> clientSubnets;

    @ConfigProperty(name = "aws.msk.kafka.version")
    String kafkaVersion;

    @Path("/clusters")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> listClusters(@QueryParam("filter") String filter) {
        Map<String, Object> headers = filter != null && !filter.isEmpty()
                ? Map.of(MSK2Constants.CLUSTERS_FILTER, filter)
                : Map.of();
        return producerTemplate.requestBodyAndHeaders(
                componentUri(listClusters),
                null,
                headers,
                ListClustersResponse.class)
                .clusterInfoList().stream()
                .map(ClusterInfo::clusterName)
                .collect(Collectors.toList());
    }

    @Path("/cluster/{clusterName}")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response createCluster(@PathParam("clusterName") String clusterName) throws Exception {
        BrokerNodeGroupInfo brokerNodeGroupInfo = BrokerNodeGroupInfo.builder()
                .clientSubnets(clientSubnets)
                .instanceType("kafka.t3.small")
                .build();

        CreateClusterResponse response = producerTemplate.requestBodyAndHeaders(
                componentUri(createCluster),
                null,
                Map.of(
                        MSK2Constants.CLUSTER_NAME, clusterName,
                        MSK2Constants.CLUSTER_KAFKA_VERSION, kafkaVersion,
                        MSK2Constants.BROKER_NODES_NUMBER, clientSubnets.size(),
                        MSK2Constants.BROKER_NODES_GROUP_INFO, brokerNodeGroupInfo),
                CreateClusterResponse.class);

        return Response.created(new URI("https://camel.apache.org/")).entity(response.clusterArn()).build();
    }

    @Path("/cluster/{clusterName}/pojo")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response createClusterPojo(@PathParam("clusterName") String clusterName) throws Exception {
        BrokerNodeGroupInfo brokerNodeGroupInfo = BrokerNodeGroupInfo.builder()
                .clientSubnets(clientSubnets)
                .instanceType("kafka.t3.small")
                .build();

        CreateClusterRequest request = CreateClusterRequest.builder()
                .clusterName(clusterName)
                .kafkaVersion(kafkaVersion)
                .numberOfBrokerNodes(clientSubnets.size())
                .brokerNodeGroupInfo(brokerNodeGroupInfo)
                .build();

        CreateClusterResponse response = producerTemplate.requestBody(
                componentUri(createCluster) + "&pojoRequest=true",
                request,
                CreateClusterResponse.class);

        return Response.created(new URI("https://camel.apache.org/")).entity(response.clusterArn()).build();
    }

    @Path("/cluster/describe")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String describeCluster(@QueryParam("clusterArn") String clusterArn) {
        return producerTemplate.requestBodyAndHeader(
                componentUri(describeCluster),
                null,
                MSK2Constants.CLUSTER_ARN,
                clusterArn,
                DescribeClusterResponse.class)
                .clusterInfo().stateAsString();
    }

    @Path("/cluster/describe/pojo")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String describeClusterPojo(@QueryParam("clusterArn") String clusterArn) {
        DescribeClusterRequest request = DescribeClusterRequest.builder()
                .clusterArn(clusterArn)
                .build();
        return producerTemplate.requestBody(
                componentUri(describeCluster) + "&pojoRequest=true",
                request,
                DescribeClusterResponse.class)
                .clusterInfo().stateAsString();
    }

    @Path("/cluster")
    @DELETE
    public void deleteCluster(@QueryParam("clusterArn") String clusterArn) {
        producerTemplate.requestBodyAndHeader(
                componentUri(deleteCluster),
                null,
                MSK2Constants.CLUSTER_ARN,
                clusterArn);
    }

    @Path("/cluster/pojo")
    @DELETE
    public void deleteClusterPojo(@QueryParam("clusterArn") String clusterArn) {
        DeleteClusterRequest request = DeleteClusterRequest.builder()
                .clusterArn(clusterArn)
                .build();
        producerTemplate.requestBody(
                componentUri(deleteCluster) + "&pojoRequest=true",
                request,
                Object.class);
    }

    private String componentUri(MSK2Operations operation) {
        return "aws2-msk:msk?operation=" + operation;
    }
}
