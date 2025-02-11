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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.ConfigMap;
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

@Path("/kubernetes/configmap")
@ApplicationScoped
public class KubernetesConfigMapResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/{namespace}/{configMapName}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ConfigMap getConfigMap(
            @PathParam("namespace") String namespace,
            @PathParam("configMapName") String configMapName) {

        Map<String, Object> headers = new HashMap<>();
        headers.put("componentName", "kubernetes-config-maps");
        headers.put(KubernetesConstants.KUBERNETES_NAMESPACE_NAME, namespace);
        headers.put(KubernetesConstants.KUBERNETES_CONFIGMAP_NAME, configMapName);
        headers.put(KubernetesConstants.KUBERNETES_OPERATION, KubernetesOperations.GET_CONFIGMAP_OPERATION);
        return producerTemplate.requestBodyAndHeaders("direct:start", null, headers, ConfigMap.class);
    }

    @Path("/{namespace}/{configMapName}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response createConfigMap(
            @PathParam("namespace") String namespace,
            @PathParam("configMapName") String configMapName,
            Map<String, String> data) throws Exception {

        Map<String, Object> headers = new HashMap<>();
        headers.put("componentName", "kubernetes-config-maps");
        headers.put(KubernetesConstants.KUBERNETES_NAMESPACE_NAME, namespace);
        headers.put(KubernetesConstants.KUBERNETES_CONFIGMAP_NAME, configMapName);
        headers.put(KubernetesConstants.KUBERNETES_CONFIGMAP_DATA, data);
        headers.put(KubernetesConstants.KUBERNETES_CONFIGMAPS_LABELS, Map.of("app", configMapName));
        headers.put(KubernetesConstants.KUBERNETES_OPERATION, KubernetesOperations.CREATE_CONFIGMAP_OPERATION);

        ConfigMap createdConfigMap = producerTemplate.requestBodyAndHeaders("direct:start", null, headers, ConfigMap.class);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(createdConfigMap)
                .build();
    }

    @Path("/{namespace}/{configMapName}")
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateConfigMap(
            @PathParam("namespace") String namespace,
            @PathParam("configMapName") String configMapName,
            Map<String, String> data) {

        Map<String, Object> headers = new HashMap<>();
        headers.put("componentName", "kubernetes-config-maps");
        headers.put(KubernetesConstants.KUBERNETES_NAMESPACE_NAME, namespace);
        headers.put(KubernetesConstants.KUBERNETES_CONFIGMAP_NAME, configMapName);
        headers.put(KubernetesConstants.KUBERNETES_CONFIGMAP_DATA, data);
        headers.put(KubernetesConstants.KUBERNETES_CONFIGMAPS_LABELS, Map.of("app", configMapName));
        headers.put(KubernetesConstants.KUBERNETES_OPERATION, KubernetesOperations.UPDATE_CONFIGMAP_OPERATION);

        ConfigMap updatedConfigMap = producerTemplate.requestBodyAndHeaders("direct:start", null, headers, ConfigMap.class);
        return Response.ok()
                .entity(updatedConfigMap)
                .build();
    }

    @Path("/{namespace}/{configMapName}")
    @DELETE
    public Response deleteConfigMap(
            @PathParam("namespace") String namespace,
            @PathParam("configMapName") String configMapName) {

        Map<String, Object> headers = new HashMap<>();
        headers.put("componentName", "kubernetes-config-maps");
        headers.put(KubernetesConstants.KUBERNETES_NAMESPACE_NAME, namespace);
        headers.put(KubernetesConstants.KUBERNETES_CONFIGMAP_NAME, configMapName);
        headers.put(KubernetesConstants.KUBERNETES_OPERATION, KubernetesOperations.DELETE_CONFIGMAP_OPERATION);
        producerTemplate.requestBodyAndHeaders("direct:start", null, headers);
        return Response
                .status(Response.Status.NO_CONTENT)
                .build();
    }

    @Path("/{namespace}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listConfigMaps(@PathParam("namespace") String namespace) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("componentName", "kubernetes-config-maps");
        headers.put(KubernetesConstants.KUBERNETES_NAMESPACE_NAME, namespace);
        headers.put(KubernetesConstants.KUBERNETES_OPERATION, KubernetesOperations.LIST_CONFIGMAPS);
        List<ConfigMap> configMapList = producerTemplate.requestBodyAndHeaders("direct:start", null, headers, List.class);
        return Response.ok().entity(configMapList).build();
    }

    @Path("/labels/{namespace}")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response listConfigMapsByLabels(
            @PathParam("namespace") String namespace,
            Map<String, String> labels) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("componentName", "kubernetes-config-maps");
        headers.put(KubernetesConstants.KUBERNETES_NAMESPACE_NAME, namespace);
        headers.put(KubernetesConstants.KUBERNETES_CONFIGMAPS_LABELS, labels);
        headers.put(KubernetesConstants.KUBERNETES_OPERATION, KubernetesOperations.LIST_CONFIGMAPS_BY_LABELS_OPERATION);
        List<ConfigMap> configMapList = producerTemplate.requestBodyAndHeaders("direct:start", null, headers, List.class);
        return Response.ok().entity(configMapList).build();
    }
}
