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

import io.fabric8.kubernetes.api.model.Pod;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
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
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.kubernetes.KubernetesConstants;
import org.apache.camel.component.kubernetes.KubernetesOperations;
import org.apache.camel.component.kubernetes.pods.KubernetesPodsComponent;
import org.eclipse.microprofile.config.ConfigProvider;

@Path("/kubernetes/pods")
@ApplicationScoped
public class KubernetesPodsResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Path("/{namespace}/{podName}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Pod getPod(
            @PathParam("namespace") String namespace,
            @PathParam("podName") String podName) {

        Map<String, Object> headers = new HashMap<>();
        headers.put("componentName", "kubernetes-pods");
        headers.put(KubernetesConstants.KUBERNETES_NAMESPACE_NAME, namespace);
        headers.put(KubernetesConstants.KUBERNETES_POD_NAME, podName);
        headers.put(KubernetesConstants.KUBERNETES_OPERATION, KubernetesOperations.GET_POD_OPERATION);
        return producerTemplate.requestBodyAndHeaders("direct:start", null, headers, Pod.class);
    }

    @Path("/{namespace}/{podName}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response createPod(
            @PathParam("namespace") String namespace,
            @PathParam("podName") String podName,
            @QueryParam("isAutowiredClient") boolean isAutowiredClient,
            Pod pod) throws Exception {

        String directEndpointUri = "direct:start";
        Map<String, Object> headers = new HashMap<>();
        if (isAutowiredClient) {
            headers.put("componentName", "kubernetes-pods");
        } else {
            directEndpointUri += "NoAutoWired";
            String masterUrl = ConfigProvider.getConfig()
                    .getOptionalValue("kubernetes.master", String.class)
                    .orElseGet(() -> ConfigProvider.getConfig()
                            .getOptionalValue("quarkus.kubernetes-client.api-server-url", String.class)
                            .orElse(null));

            headers.put("componentName", "kubernetes-pods-no-autowire");
            headers.put("masterUrl", masterUrl);
        }

        headers.put(KubernetesConstants.KUBERNETES_NAMESPACE_NAME, namespace);
        headers.put(KubernetesConstants.KUBERNETES_POD_NAME, podName);
        headers.put(KubernetesConstants.KUBERNETES_POD_SPEC, pod.getSpec());
        headers.put(KubernetesConstants.KUBERNETES_PODS_LABELS, Map.of("app", podName));
        headers.put(KubernetesConstants.KUBERNETES_OPERATION, KubernetesOperations.CREATE_POD_OPERATION);

        Pod createdPod = producerTemplate.requestBodyAndHeaders(directEndpointUri, null, headers, Pod.class);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(createdPod)
                .build();
    }

    @Path("/{namespace}/{podName}")
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    public Response updatePod(
            @PathParam("namespace") String namespace,
            @PathParam("podName") String podName,
            Pod pod) {

        Map<String, Object> headers = new HashMap<>();
        headers.put("componentName", "kubernetes-pods");
        headers.put(KubernetesConstants.KUBERNETES_NAMESPACE_NAME, namespace);
        headers.put(KubernetesConstants.KUBERNETES_POD_NAME, podName);
        headers.put(KubernetesConstants.KUBERNETES_POD_SPEC, pod.getSpec());
        headers.put(KubernetesConstants.KUBERNETES_PODS_LABELS, Map.of("app", podName));
        headers.put(KubernetesConstants.KUBERNETES_OPERATION, KubernetesOperations.UPDATE_POD_OPERATION);

        Pod updatedPod = producerTemplate.requestBodyAndHeaders("direct:start", null, headers, Pod.class);
        return Response.ok().entity(updatedPod).build();
    }

    @Path("/{namespace}/{podName}")
    @DELETE
    public Response deletePod(
            @PathParam("namespace") String namespace,
            @PathParam("podName") String podName) {

        Map<String, Object> headers = new HashMap<>();
        headers.put("componentName", "kubernetes-pods");
        headers.put(KubernetesConstants.KUBERNETES_NAMESPACE_NAME, namespace);
        headers.put(KubernetesConstants.KUBERNETES_POD_NAME, podName);
        headers.put(KubernetesConstants.KUBERNETES_OPERATION, KubernetesOperations.DELETE_POD_OPERATION);
        producerTemplate.requestBodyAndHeaders("direct:start", null, headers);
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @Path("/{namespace}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listPods(@PathParam("namespace") String namespace) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("componentName", "kubernetes-pods");
        headers.put(KubernetesConstants.KUBERNETES_NAMESPACE_NAME, namespace);
        headers.put(KubernetesConstants.KUBERNETES_OPERATION, KubernetesOperations.LIST_PODS_OPERATION);
        List<Pod> podList = producerTemplate.requestBodyAndHeaders("direct:start", null, headers, List.class);
        return Response.ok().entity(podList).build();
    }

    @Path("/labels/{namespace}")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response listPodsByLabels(
            @PathParam("namespace") String namespace,
            Map<String, String> labels) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("componentName", "kubernetes-pods");
        headers.put(KubernetesConstants.KUBERNETES_NAMESPACE_NAME, namespace);
        headers.put(KubernetesConstants.KUBERNETES_PODS_LABELS, labels);
        headers.put(KubernetesConstants.KUBERNETES_OPERATION, KubernetesOperations.LIST_PODS_BY_LABELS_OPERATION);
        List<Pod> podList = producerTemplate.requestBodyAndHeaders("direct:start", null, headers, List.class);
        return Response.ok().entity(podList).build();
    }

    @Path("/events")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEvents() {
        Pod pod = consumerTemplate.receiveBody("seda:podEvents", 10000, Pod.class);
        return Response.ok().entity(pod).build();
    }

    @Named("kubernetes-pods-no-autowire")
    KubernetesPodsComponent kubernetesPodsComponent() {
        KubernetesPodsComponent component = new KubernetesPodsComponent();
        component.setAutowiredEnabled(false);
        return component;
    }
}
