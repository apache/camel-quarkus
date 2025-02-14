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
package org.apache.camel.quarkus.component.kubernetes.it;

import java.net.URI;
import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.kubernetes.KubernetesConstants;
import org.apache.camel.component.kubernetes.KubernetesOperations;

@Path("/kubernetes/deployment")
@ApplicationScoped
public class KubernetesDeploymentResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/{namespace}/{name}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Deployment getDeployment(
            @PathParam("namespace") String namespace,
            @PathParam("name") String name) {

        Map<String, Object> headers = Map.of(
                "componentName", "kubernetes-deployments",
                KubernetesConstants.KUBERNETES_NAMESPACE_NAME, namespace,
                KubernetesConstants.KUBERNETES_DEPLOYMENT_NAME, name,
                KubernetesConstants.KUBERNETES_OPERATION, KubernetesOperations.GET_DEPLOYMENT);
        return producerTemplate.requestBodyAndHeaders("direct:start", null, headers, Deployment.class);
    }

    @Path("/{namespace}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response createDeployment(
            @PathParam("namespace") String namespace,
            Deployment deployment) throws Exception {

        Map<String, Object> headers = Map.of(
                "componentName", "kubernetes-deployments",
                KubernetesConstants.KUBERNETES_NAMESPACE_NAME, namespace,
                KubernetesConstants.KUBERNETES_DEPLOYMENT_NAME, deployment.getMetadata().getName(),
                KubernetesConstants.KUBERNETES_DEPLOYMENT_SPEC, deployment.getSpec(),
                KubernetesConstants.KUBERNETES_DEPLOYMENTS_LABELS, Map.of("app", deployment.getMetadata().getName()),
                KubernetesConstants.KUBERNETES_OPERATION, KubernetesOperations.CREATE_DEPLOYMENT);

        Deployment createdDeployment = producerTemplate.requestBodyAndHeaders("direct:start", null, headers, Deployment.class);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(createdDeployment)
                .build();
    }

    @Path("/{namespace}")
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateDeployment(
            @PathParam("namespace") String namespace,
            Deployment deployment) {

        Map<String, Object> headers = Map.of(
                "componentName", "kubernetes-deployments",
                KubernetesConstants.KUBERNETES_NAMESPACE_NAME, namespace,
                KubernetesConstants.KUBERNETES_DEPLOYMENT_NAME, deployment.getMetadata().getName(),
                KubernetesConstants.KUBERNETES_DEPLOYMENT_SPEC, deployment.getSpec(),
                KubernetesConstants.KUBERNETES_DEPLOYMENTS_LABELS, Map.of("app", deployment.getMetadata().getName()),
                KubernetesConstants.KUBERNETES_OPERATION, KubernetesOperations.UPDATE_DEPLOYMENT);

        Deployment updatedDeployment = producerTemplate.requestBodyAndHeaders("direct:start", null, headers, Deployment.class);
        return Response.ok().entity(updatedDeployment).build();
    }

    @Path("/{namespace}/{name}")
    @DELETE
    public Response deleteDeployment(
            @PathParam("namespace") String namespace,
            @PathParam("name") String name) {

        Map<String, Object> headers = Map.of(
                "componentName", "kubernetes-deployments",
                KubernetesConstants.KUBERNETES_NAMESPACE_NAME, namespace,
                KubernetesConstants.KUBERNETES_DEPLOYMENT_NAME, name,
                KubernetesConstants.KUBERNETES_OPERATION, KubernetesOperations.DELETE_DEPLOYMENT);
        producerTemplate.requestBodyAndHeaders("direct:start", null, headers);
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @Path("/{namespace}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listDeployments(@PathParam("namespace") String namespace) {
        Map<String, Object> headers = Map.of(
                "componentName", "kubernetes-deployments",
                KubernetesConstants.KUBERNETES_NAMESPACE_NAME, namespace,
                KubernetesConstants.KUBERNETES_OPERATION, KubernetesOperations.LIST_DEPLOYMENTS);
        List<Deployment> list = producerTemplate.requestBodyAndHeaders("direct:start", null, headers, List.class);
        return Response.ok().entity(list).build();
    }

    @Path("/labels/{namespace}")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response listDeploymentsByLabels(
            @PathParam("namespace") String namespace,
            Map<String, String> labels) {
        Map<String, Object> headers = Map.of(
                "componentName", "kubernetes-deployments",
                KubernetesConstants.KUBERNETES_NAMESPACE_NAME, namespace,
                KubernetesConstants.KUBERNETES_DEPLOYMENTS_LABELS, labels,
                KubernetesConstants.KUBERNETES_OPERATION, KubernetesOperations.LIST_DEPLOYMENTS_BY_LABELS_OPERATION);
        List<Deployment> list = producerTemplate.requestBodyAndHeaders("direct:start", null, headers, List.class);
        return Response.ok().entity(list).build();
    }

    @Path("/{namespace}/{name}/{replicas}")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response scaleDeployment(
            @PathParam("namespace") String namespace,
            @PathParam("name") String name,
            @PathParam("replicas") String replicas) throws Exception {

        Map<String, Object> headers = Map.of(
                "componentName", "kubernetes-deployments",
                KubernetesConstants.KUBERNETES_NAMESPACE_NAME, namespace,
                KubernetesConstants.KUBERNETES_DEPLOYMENT_NAME, name,
                KubernetesConstants.KUBERNETES_DEPLOYMENT_REPLICAS, replicas,
                KubernetesConstants.KUBERNETES_OPERATION, KubernetesOperations.SCALE_DEPLOYMENT);

        Integer updatedReplicas = producerTemplate.requestBodyAndHeaders("direct:start", null, headers, Integer.class);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(updatedReplicas)
                .build();
    }
}
