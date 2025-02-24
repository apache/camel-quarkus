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

import java.util.concurrent.atomic.AtomicReference;

import io.fabric8.kubernetes.client.KubernetesClient;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.component.kubernetes.config_maps.vault.ConfigmapsReloadTriggerTask;
import org.apache.camel.component.kubernetes.customresources.KubernetesCustomResourcesConsumer;
import org.apache.camel.component.kubernetes.secrets.vault.SecretsReloadTriggerTask;
import org.apache.camel.impl.event.CamelContextReloadedEvent;
import org.apache.camel.util.ObjectHelper;

@Path("/kubernetes")
public class KubernetesResource {
    @Inject
    CamelContext context;

    @Inject
    KubernetesClient client;

    void onReload(@Observes CamelContextReloadedEvent event) {
        String eventSource = event.getAction().getClass().getName();
        if (eventSource.startsWith(ConfigmapsReloadTriggerTask.class.getName())) {
            KubernetesConfigMapResource.CONTEXT_RELOADED.set(true);
        }

        if (eventSource.startsWith(SecretsReloadTriggerTask.class.getName())) {
            KubernetesSecretResource.CONTEXT_RELOADED.set(true);
        }
    }

    @Path("/route/{routeId}/start")
    @POST
    public void startRoute(
            @PathParam("routeId") String routeId,
            @QueryParam("namespace") String namespace) throws Exception {

        AtomicReference<String> reference = context.getRegistry().lookupByNameAndType("namespace", AtomicReference.class);
        reference.set(namespace);

        // Special case for CRD consumer as it needs the namespace set in its configuration before startup
        if (routeId.equals("custom-resource-listener")) {
            KubernetesCustomResourcesConsumer consumer = (KubernetesCustomResourcesConsumer) context.getRoute(routeId)
                    .getConsumer();
            consumer.getEndpoint().getKubernetesConfiguration().setNamespace(namespace);
        }

        context.getRouteController().startRoute(routeId);
    }

    @Path("/route/{routeId}/stop")
    @POST
    public void stopRoute(@PathParam("routeId") String routeId) throws Exception {
        AtomicReference<String> reference = context.getRegistry().lookupByNameAndType("namespace", AtomicReference.class);
        reference.set(null);
        context.getRouteController().stopRoute(routeId);
    }

    @Path("/default/namespace")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getDefaultNamespace() {
        String namespace = ObjectHelper.isEmpty(client.getNamespace()) ? "default" : client.getNamespace();
        return Response.ok().entity(namespace).build();
    }
}
