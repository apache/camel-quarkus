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
import java.util.concurrent.atomic.AtomicBoolean;

import io.fabric8.kubernetes.api.model.Secret;
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

@Path("/kubernetes/secret")
@ApplicationScoped
public class KubernetesSecretResource {
    static final AtomicBoolean CONTEXT_RELOADED = new AtomicBoolean(false);

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/{namespace}/{name}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Secret getSecret(
            @PathParam("namespace") String namespace,
            @PathParam("name") String name) {

        Map<String, Object> headers = Map.of(
                "componentName", "kubernetes-secrets",
                KubernetesConstants.KUBERNETES_NAMESPACE_NAME, namespace,
                KubernetesConstants.KUBERNETES_SECRET_NAME, name,
                KubernetesConstants.KUBERNETES_OPERATION, KubernetesOperations.GET_SECRET_OPERATION);
        return producerTemplate.requestBodyAndHeaders("direct:start", null, headers, Secret.class);
    }

    @Path("/{namespace}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response createSecret(
            @PathParam("namespace") String namespace,
            Secret secret) throws Exception {

        Map<String, String> labelsAndAnnotations = Map.of("app", secret.getMetadata().getName());
        Map<String, Object> headers = Map.of(
                "componentName", "kubernetes-secrets",
                KubernetesConstants.KUBERNETES_NAMESPACE_NAME, namespace,
                KubernetesConstants.KUBERNETES_SECRET_NAME, secret.getMetadata().getName(),
                KubernetesConstants.KUBERNETES_SECRET, secret,
                KubernetesConstants.KUBERNETES_SECRETS_LABELS, labelsAndAnnotations,
                KubernetesConstants.KUBERNETES_SECRETS_ANNOTATIONS, labelsAndAnnotations,
                KubernetesConstants.KUBERNETES_OPERATION, KubernetesOperations.CREATE_SECRET_OPERATION);

        Secret createdSecret = producerTemplate.requestBodyAndHeaders("direct:start", null, headers, Secret.class);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(createdSecret)
                .build();
    }

    @Path("/{namespace}")
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateSecret(
            @PathParam("namespace") String namespace,
            Secret secret) {

        Map<String, String> labelsAndAnnotations = Map.of("app", secret.getMetadata().getName());
        Map<String, Object> headers = Map.of(
                "componentName", "kubernetes-secrets",
                KubernetesConstants.KUBERNETES_NAMESPACE_NAME, namespace,
                KubernetesConstants.KUBERNETES_SECRET_NAME, secret.getMetadata().getName(),
                KubernetesConstants.KUBERNETES_SECRET, secret,
                KubernetesConstants.KUBERNETES_SECRETS_LABELS, labelsAndAnnotations,
                KubernetesConstants.KUBERNETES_SECRETS_ANNOTATIONS, labelsAndAnnotations,
                KubernetesConstants.KUBERNETES_OPERATION, KubernetesOperations.UPDATE_SECRET_OPERATION);

        Secret updatedSecret = producerTemplate.requestBodyAndHeaders("direct:start", null, headers, Secret.class);
        return Response.ok()
                .entity(updatedSecret)
                .build();
    }

    @Path("/{namespace}/{name}")
    @DELETE
    public Response deleteSecret(
            @PathParam("namespace") String namespace,
            @PathParam("name") String name) {

        Map<String, Object> headers = Map.of(
                "componentName", "kubernetes-secrets",
                KubernetesConstants.KUBERNETES_NAMESPACE_NAME, namespace,
                KubernetesConstants.KUBERNETES_SECRET_NAME, name,
                KubernetesConstants.KUBERNETES_OPERATION, KubernetesOperations.DELETE_SECRET_OPERATION);
        producerTemplate.requestBodyAndHeaders("direct:start", null, headers);
        return Response
                .status(Response.Status.NO_CONTENT)
                .build();
    }

    @Path("/{namespace}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listSecrets(@PathParam("namespace") String namespace) {
        Map<String, Object> headers = Map.of(
                "componentName", "kubernetes-secrets",
                KubernetesConstants.KUBERNETES_NAMESPACE_NAME, namespace,
                KubernetesConstants.KUBERNETES_OPERATION, KubernetesOperations.LIST_SECRETS);
        List<Secret> list = producerTemplate.requestBodyAndHeaders("direct:start", null, headers, List.class);
        return Response.ok().entity(list).build();
    }

    @Path("/labels/{namespace}")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response listSecretsByLabels(
            @PathParam("namespace") String namespace,
            Map<String, String> labels) {
        Map<String, Object> headers = Map.of(
                "componentName", "kubernetes-secrets",
                KubernetesConstants.KUBERNETES_NAMESPACE_NAME, namespace,
                KubernetesConstants.KUBERNETES_SECRETS_LABELS, labels,
                KubernetesConstants.KUBERNETES_OPERATION, KubernetesOperations.LIST_SECRETS_BY_LABELS_OPERATION);
        List<Secret> list = producerTemplate.requestBodyAndHeaders("direct:start", null, headers, List.class);
        return Response.ok().entity(list).build();
    }

    @Path("/property/resolve")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response resolvePropertyFromSecret() {
        String result = producerTemplate.requestBody("direct:secretProperty", null, String.class);
        return Response.ok().entity(result).build();
    }

    @Path("/context/reload/state")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response contextReloadState() {
        String result = CONTEXT_RELOADED.get() ? "reloaded" : "not-reloaded";
        return Response.ok().entity(result).build();
    }
}
