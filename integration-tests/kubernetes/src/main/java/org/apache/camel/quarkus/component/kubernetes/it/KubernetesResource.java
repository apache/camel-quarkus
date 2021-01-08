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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodSpec;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.kubernetes.KubernetesConstants;

@Path("/kubernetes")
@ApplicationScoped
public class KubernetesResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/pod/{namespace}/{podName}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String readPod(@PathParam("namespace") String namespace, @PathParam("podName") String podName) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(KubernetesConstants.KUBERNETES_NAMESPACE_NAME, namespace);
        headers.put(KubernetesConstants.KUBERNETES_POD_NAME, podName);
        final Pod pod = producerTemplate.requestBodyAndHeaders(
                "kubernetes-pods:///?kubernetesClient=#kubernetesClient&operation=getPod", null, headers, Pod.class);
        return pod.getMetadata().getName();
    }

    @Path("/pod/{namespace}/{podName}")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response createPod(@PathParam("namespace") String namespace, @PathParam("podName") String podName) throws Exception {
        Map<String, Object> headers = new HashMap<>();
        headers.put(KubernetesConstants.KUBERNETES_NAMESPACE_NAME, namespace);
        headers.put(KubernetesConstants.KUBERNETES_POD_NAME, podName);
        headers.put(KubernetesConstants.KUBERNETES_POD_SPEC, createPodSpec(podName));
        producerTemplate.requestBodyAndHeaders("kubernetes-pods:///?kubernetesClient=#kubernetesClient&operation=createPod",
                null,
                headers, Pod.class);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .build();
    }

    @Path("/pod/{namespace}/{podName}")
    @DELETE
    public Response deletePod(@PathParam("namespace") String namespace, @PathParam("podName") String podName) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(KubernetesConstants.KUBERNETES_NAMESPACE_NAME, namespace);
        headers.put(KubernetesConstants.KUBERNETES_POD_NAME, podName);
        producerTemplate.requestBodyAndHeaders("kubernetes-pods:///?kubernetesClient=#kubernetesClient&operation=deletePod",
                null,
                headers);
        return Response
                .status(Response.Status.NO_CONTENT)
                .build();
    }

    private PodSpec createPodSpec(String containerName) {
        PodSpec podSpec = new PodSpec();

        Container container = new Container();
        container.setImage("busybox:latest");
        container.setName(containerName);

        List<Container> containers = new ArrayList<>();
        containers.add(container);

        podSpec.setContainers(containers);

        return podSpec;
    }
}
