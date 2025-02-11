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
import java.util.Map;

import io.fabric8.kubernetes.api.model.Namespace;
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
import org.apache.camel.component.kubernetes.KubernetesConstants;
import org.apache.camel.component.kubernetes.KubernetesOperations;

@Path("/kubernetes/namespace")
@ApplicationScoped
public class KubernetesNamespaceResource {
    @Inject
    ProducerTemplate producerTemplate;

    @Path("/{name}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Namespace getNamespace(
            @QueryParam("port") int port,
            @PathParam("name") String name) {

        Map<String, Object> headers = new HashMap<>();
        headers.put("port", port);
        headers.put("componentName", "kubernetes-namespaces");
        headers.put(KubernetesConstants.KUBERNETES_NAMESPACE_NAME, name);
        headers.put(KubernetesConstants.KUBERNETES_OPERATION, KubernetesOperations.GET_NAMESPACE_OPERATION);
        return producerTemplate.requestBodyAndHeaders("direct:start", null, headers, Namespace.class);
    }

    @Path("/{name}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response createNamespace(
            @QueryParam("port") int port,
            @PathParam("name") String name) throws Exception {

        Map<String, Object> headers = new HashMap<>();
        headers.put("port", port);
        headers.put("componentName", "kubernetes-namespaces");
        headers.put(KubernetesConstants.KUBERNETES_NAMESPACE_NAME, name);
        headers.put(KubernetesConstants.KUBERNETES_OPERATION, KubernetesOperations.CREATE_NAMESPACE_OPERATION);

        Namespace createdNamespace = producerTemplate.requestBodyAndHeaders("direct:start", null, headers, Namespace.class);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(createdNamespace)
                .build();
    }

    @Path("/{name}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Boolean deleteNamespace(
            @QueryParam("port") int port,
            @PathParam("name") String name) {

        Map<String, Object> headers = new HashMap<>();
        headers.put("port", port);
        headers.put("componentName", "kubernetes-namespaces");
        headers.put(KubernetesConstants.KUBERNETES_NAMESPACE_NAME, name);
        headers.put(KubernetesConstants.KUBERNETES_OPERATION, KubernetesOperations.DELETE_NAMESPACE_OPERATION);
        return producerTemplate.requestBodyAndHeaders("direct:start", null, headers, Boolean.class);
    }
}
