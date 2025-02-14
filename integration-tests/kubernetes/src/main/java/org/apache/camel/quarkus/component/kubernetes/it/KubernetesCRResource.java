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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
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
import org.apache.camel.util.json.JsonObject;

@Path("/kubernetes/customresource")
@ApplicationScoped
public class KubernetesCRResource {
    public static final String CRD_NAME = "testcr";
    public static final String CRD_GROUP = "test.com";
    public static final String CRD_VERSION = "v1";
    public static final String CRD_PLURAL = "testcrs";
    public static final String CRD_SCOPE = "Namespaced";

    @Inject
    ProducerTemplate producerTemplate;

    private final Map<String, String> common = Map.of(
            "componentName", "kubernetes-custom-resources",
            KubernetesConstants.KUBERNETES_CRD_NAME, CRD_NAME,
            KubernetesConstants.KUBERNETES_CRD_PLURAL, CRD_PLURAL,
            KubernetesConstants.KUBERNETES_CRD_GROUP, CRD_GROUP,
            KubernetesConstants.KUBERNETES_CRD_VERSION, CRD_VERSION,
            KubernetesConstants.KUBERNETES_CRD_SCOPE, CRD_SCOPE);

    @Path("/{namespace}/{name}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject getCustomResource(
            @PathParam("namespace") String namespace,
            @PathParam("name") String name) {

        Map<String, Object> headers = new HashMap<>(common);
        headers.putAll(Map.of(
                KubernetesConstants.KUBERNETES_CRD_INSTANCE_NAME, name,
                KubernetesConstants.KUBERNETES_OPERATION, KubernetesOperations.GET_CUSTOMRESOURCE,
                KubernetesConstants.KUBERNETES_NAMESPACE_NAME, namespace));

        return producerTemplate.requestBodyAndHeaders("direct:start", null, headers, JsonObject.class);
    }

    @Path("/{namespace}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public GenericKubernetesResource createCustomResource(@PathParam("namespace") String namespace, String instance) {
        Map<String, Object> headers = new HashMap<>(common);
        headers.putAll(Map.of(
                KubernetesConstants.KUBERNETES_OPERATION, KubernetesOperations.CREATE_CUSTOMRESOURCE,
                KubernetesConstants.KUBERNETES_CRD_INSTANCE, instance,
                KubernetesConstants.KUBERNETES_NAMESPACE_NAME, namespace));
        return producerTemplate.requestBodyAndHeaders("direct:start", null, headers, GenericKubernetesResource.class);
    }

    @Path("/{namespace}/{name}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateCustomResource(
            @PathParam("namespace") String namespace,
            @PathParam("name") String name,
            String instance) {

        Map<String, Object> headers = new HashMap<>(common);
        headers.putAll(Map.of(
                KubernetesConstants.KUBERNETES_CRD_INSTANCE_NAME, name,
                KubernetesConstants.KUBERNETES_OPERATION, KubernetesOperations.UPDATE_CUSTOMRESOURCE,
                KubernetesConstants.KUBERNETES_CRD_INSTANCE, instance,
                KubernetesConstants.KUBERNETES_NAMESPACE_NAME, namespace));
        GenericKubernetesResource updated = producerTemplate.requestBodyAndHeaders("direct:start", null, headers,
                GenericKubernetesResource.class);
        return Response.ok()
                .entity(updated)
                .build();
    }

    @Path("/{namespace}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listCustomResources(@PathParam("namespace") String namespace) {
        Map<String, Object> headers = new HashMap<>(common);
        headers.putAll(Map.of(
                KubernetesConstants.KUBERNETES_OPERATION, KubernetesOperations.LIST_CUSTOMRESOURCES,
                KubernetesConstants.KUBERNETES_NAMESPACE_NAME, namespace));
        List<ConfigMap> list = producerTemplate.requestBodyAndHeaders("direct:start", null, headers, List.class);
        return Response.ok().entity(list).build();
    }

    @Path("/labels/{namespace}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listCustomResourcesByLabels(@PathParam("namespace") String namespace, Map<String, String> labels) {
        Map<String, Object> headers = new HashMap<>(common);
        headers.putAll(Map.of(
                KubernetesConstants.KUBERNETES_CRD_LABELS, labels,
                KubernetesConstants.KUBERNETES_OPERATION, KubernetesOperations.LIST_CUSTOMRESOURCES_BY_LABELS_OPERATION,
                KubernetesConstants.KUBERNETES_NAMESPACE_NAME, namespace));
        List<GenericKubernetesResource> list = producerTemplate.requestBodyAndHeaders("direct:start", null, headers,
                List.class);
        return Response.ok().entity(list).build();
    }

    @Path("/{namespace}/{name}")
    @DELETE
    public Response deleteCustomResource(
            @PathParam("namespace") String namespace,
            @PathParam("name") String name) {
        Map<String, Object> headers = new HashMap<>(common);
        headers.putAll(Map.of(
                KubernetesConstants.KUBERNETES_OPERATION, KubernetesOperations.DELETE_CUSTOMRESOURCE,
                KubernetesConstants.KUBERNETES_CRD_INSTANCE_NAME, name,
                KubernetesConstants.KUBERNETES_NAMESPACE_NAME, namespace));
        producerTemplate.requestBodyAndHeaders("direct:start", null, headers);
        return Response
                .status(Response.Status.NO_CONTENT)
                .build();
    }
}
