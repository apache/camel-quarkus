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
package org.apache.camel.quarkus.component.aws2.eks.it;

import java.util.List;
import java.util.Map;

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
import org.apache.camel.component.aws2.eks.EKS2Constants;
import org.apache.camel.component.aws2.eks.EKS2Operations;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.services.eks.model.Cluster;
import software.amazon.awssdk.services.eks.model.CreateClusterResponse;
import software.amazon.awssdk.services.eks.model.DeleteClusterResponse;
import software.amazon.awssdk.services.eks.model.DescribeClusterResponse;
import software.amazon.awssdk.services.eks.model.ListClustersResponse;
import software.amazon.awssdk.services.eks.model.VpcConfigRequest;

@Path("/aws2-eks")
@ApplicationScoped
public class Aws2EksResource {

    @Inject
    ProducerTemplate producerTemplate;

    @ConfigProperty(name = "aws.eks.role.arn")
    String roleArn;

    @ConfigProperty(name = "aws.eks.subnets")
    List<String> subnets;

    @Path("/clusters")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listClusters(@QueryParam("maxResults") Integer maxResults) {
        Map<String, Object> headers = maxResults != null
                ? Map.of(EKS2Constants.MAX_RESULTS, maxResults)
                : Map.of();

        ListClustersResponse response = producerTemplate.requestBodyAndHeaders(
                componentUri(EKS2Operations.listClusters),
                null,
                headers,
                ListClustersResponse.class);

        return Response.ok(response.clusters()).build();
    }

    @Path("/clusters/{clusterName}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response describeCluster(@PathParam("clusterName") String clusterName) {
        DescribeClusterResponse response = producerTemplate.requestBodyAndHeader(
                componentUri(EKS2Operations.describeCluster),
                null,
                EKS2Constants.CLUSTER_NAME,
                clusterName,
                DescribeClusterResponse.class);

        if (response.cluster() == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        Cluster cluster = response.cluster();
        Map<String, String> result = Map.of(
                "clusterName", cluster.name(),
                "clusterArn", cluster.arn(),
                "clusterStatus", cluster.statusAsString());
        return Response.ok(result).build();
    }

    @Path("/clusters")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response createCluster(@QueryParam("clusterName") String clusterName) {
        Map<String, Object> headers = Map.of(
                EKS2Constants.CLUSTER_NAME, clusterName,
                EKS2Constants.ROLE_ARN, roleArn,
                EKS2Constants.VPC_CONFIG, VpcConfigRequest.builder().subnetIds(subnets).build());

        CreateClusterResponse response = producerTemplate.requestBodyAndHeaders(
                componentUri(EKS2Operations.createCluster),
                null,
                headers,
                CreateClusterResponse.class);

        return Response.ok(response.cluster().arn()).build();
    }

    @Path("/clusters/{clusterName}")
    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public Response deleteCluster(@PathParam("clusterName") String clusterName) {
        DeleteClusterResponse response = producerTemplate.requestBodyAndHeader(
                componentUri(EKS2Operations.deleteCluster),
                null,
                EKS2Constants.CLUSTER_NAME,
                clusterName,
                DeleteClusterResponse.class);

        return Response.ok(response.cluster().arn()).build();
    }

    private String componentUri(EKS2Operations operation) {
        return "aws2-eks://test?operation=" + operation;
    }
}
