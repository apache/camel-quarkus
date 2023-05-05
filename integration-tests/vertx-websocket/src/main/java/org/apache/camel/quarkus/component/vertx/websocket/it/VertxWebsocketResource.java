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
package org.apache.camel.quarkus.component.vertx.websocket.it;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.WebSocketConnectOptions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.quarkus.component.vertx.websocket.VertxWebsocketRecorder;
import org.apache.camel.support.jsse.KeyStoreParameters;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.jsse.TrustManagersParameters;

@Path("/vertx-websocket")
@ApplicationScoped
public class VertxWebsocketResource {

    @Inject
    CamelContext context;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/run")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public void run(
            @QueryParam("endpointUri") String endpointUri,
            @QueryParam("camelHeader") List<String> camelHeaders,
            String message) {

        Map<String, Object> headers = new HashMap<>();
        camelHeaders.stream()
                .map(header -> header.split(":"))
                .forEach(headerParts -> {
                    headers.put(headerParts[0], headerParts[1]);
                });

        producerTemplate.sendBodyAndHeaders(endpointUri, message, headers);
    }

    @Path("/messages")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getMessages() {
        StringJoiner joiner = new StringJoiner(",");
        QuarkusWebsocketResource.getMessages()
                .stream()
                .forEach(joiner::add);
        return joiner.toString();
    }

    @Path("/messages")
    @DELETE
    public void deleteMessages() {
        QuarkusWebsocketResource.clearMessages();
    }

    @Path("/seda/{endpointUri}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String consumeFromSedaEndpoint(@PathParam("endpointUri") String endpointUri) {
        return consumerTemplate.receiveBody(endpointUri, 5000, String.class);
    }

    @POST
    @Path("manageClientConsumer/enable/{enable}")
    public void manageClientConsumer(@PathParam("enable") boolean enable) throws Exception {
        String routeId = "consumeAsClientRoute";
        if (enable) {
            context.getRouteController().startRoute(routeId);
        } else {
            context.getRouteController().stopRoute(routeId);
        }
    }

    @Path("/default/port")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public int getDefaultPort(String scheme) {
        String uri = "vertx-websocket:" + scheme + "localhost/test";
        VertxWebsocketRecorder.QuarkusVertxWebsocketEndpoint endpoint = context.getEndpoint(uri,
                VertxWebsocketRecorder.QuarkusVertxWebsocketEndpoint.class);
        WebSocketConnectOptions connectOptions = endpoint.getWebSocketConnectOptions(new HttpClientOptions());
        return connectOptions.getPort();
    }

    @Named
    public SSLContextParameters clientSSLContextParameters() {
        KeyStoreParameters truststoreParameters = new KeyStoreParameters();
        truststoreParameters.setResource("/truststore.p12");
        truststoreParameters.setPassword("changeit");

        TrustManagersParameters trustManagersParameters = new TrustManagersParameters();
        trustManagersParameters.setKeyStore(truststoreParameters);
        SSLContextParameters sslContextParameters = new SSLContextParameters();
        sslContextParameters.setTrustManagers(trustManagersParameters);

        return sslContextParameters;
    }
}
