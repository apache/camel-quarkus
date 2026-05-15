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
package org.apache.camel.quarkus.component.aws2.ecs.it;

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
import org.apache.camel.component.aws2.ecs.ECS2Constants;
import org.apache.camel.component.aws2.ecs.ECS2Operations;
import software.amazon.awssdk.services.ecs.model.Cluster;
import software.amazon.awssdk.services.ecs.model.CreateClusterResponse;
import software.amazon.awssdk.services.ecs.model.DeleteClusterResponse;
import software.amazon.awssdk.services.ecs.model.DescribeClustersResponse;
import software.amazon.awssdk.services.ecs.model.ListClustersResponse;

@Path("/aws2-ecs")
@ApplicationScoped
public class Aws2EcsResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/clusters")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listClusters(@QueryParam("maxResults") Integer maxResults) {
        Map<String, Object> headers = maxResults != null
                ? Map.of(ECS2Constants.MAX_RESULTS, maxResults)
                : Map.of();

        ListClustersResponse response = producerTemplate.requestBodyAndHeaders(
                componentUri(ECS2Operations.listClusters),
                null,
                headers,
                ListClustersResponse.class);

        return Response.ok(response.clusterArns()).build();
    }

    @Path("/clusters/{clusterName}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response describeCluster(@PathParam("clusterName") String clusterName) {
        DescribeClustersResponse response = producerTemplate.requestBodyAndHeader(
                componentUri(ECS2Operations.describeCluster),
                null,
                ECS2Constants.CLUSTER_NAME,
                clusterName,
                DescribeClustersResponse.class);

        if (response.clusters().isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        Cluster cluster = response.clusters().get(0);
        Map<String, String> result = Map.of(
                "clusterName", cluster.clusterName(),
                "clusterArn", cluster.clusterArn());
        return Response.ok(result).build();
    }

    @Path("/clusters")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response createCluster(@QueryParam("clusterName") String clusterName) {
        CreateClusterResponse response = producerTemplate.requestBodyAndHeader(
                componentUri(ECS2Operations.createCluster),
                null,
                ECS2Constants.CLUSTER_NAME,
                clusterName,
                CreateClusterResponse.class);

        return Response.ok(response.cluster().clusterArn()).build();
    }

    @Path("/clusters/{clusterName}")
    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public Response deleteCluster(@PathParam("clusterName") String clusterName) {
        DeleteClusterResponse response = producerTemplate.requestBodyAndHeader(
                componentUri(ECS2Operations.deleteCluster),
                null,
                ECS2Constants.CLUSTER_NAME,
                clusterName,
                DeleteClusterResponse.class);

        return Response.ok(response.cluster().clusterArn()).build();
    }

    private String componentUri(ECS2Operations operation) {
        return "aws2-ecs://test?operation=" + operation;
    }
}
