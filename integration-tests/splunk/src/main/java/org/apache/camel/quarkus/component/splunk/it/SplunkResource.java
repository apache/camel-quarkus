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
package org.apache.camel.quarkus.component.splunk.it;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.splunk.SplunkComponent;
import org.apache.camel.component.splunk.SplunkConfiguration;
import org.apache.camel.component.splunk.event.SplunkEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/splunk")
@ApplicationScoped
public class SplunkResource {

    public static final String PARAM_REMOTE_PORT = "org.apache.camel.quarkus.component.splunk.it.SplunkResource_remotePort";
    public static final String PARAM_TCP_PORT = "org.apache.camel.quarkus.component.splunk.it.SplunkResource_tcpPort";
    public static final String SOURCE = "test";
    public static final String SOURCE_TYPE = "testSource";
    public static final int LOCAL_TCP_PORT = 9998;

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @ConfigProperty(name = PARAM_REMOTE_PORT)
    Integer port;

    @ConfigProperty(name = PARAM_TCP_PORT)
    Integer tcpPort;

    @Inject
    CamelContext camelContext;

    @Named
    SplunkComponent splunk() {
        SplunkComponent component = new SplunkComponent();
        component.setSplunkConfigurationFactory(parameters -> new SplunkConfiguration());

        return component;
    }

    @Path("/normal")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public List normal(String search) throws Exception {
        String url = String.format(
                "splunk://normal?username=admin&password=changeit&scheme=http&port=%d&delay=5000&initEarliestTime=-10s&search="
                        + search,
                port);

        final SplunkEvent m1 = consumerTemplate.receiveBody(url, 1000, SplunkEvent.class);
        final SplunkEvent m2 = consumerTemplate.receiveBody(url, 1000, SplunkEvent.class);
        final SplunkEvent m3 = consumerTemplate.receiveBody(url, 1000, SplunkEvent.class);

        List result = Arrays.stream(new SplunkEvent[] { m1, m2, m3 })
                .map(m -> m.getEventData().entrySet().stream()
                        .filter(e -> !e.getKey().startsWith("_"))
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (v1, v2) -> v1)))
                .collect(Collectors.toList());

        return result;
    }

    @Path("/savedSearch")
    @POST
    public String savedSearch(String name) throws Exception {
        String url = String.format(
                "splunk://savedsearch?username=admin&password=changeit&scheme=http&port=%d&delay=500&initEarliestTime=-1m&savedsearch=%s",
                port, name);

        final SplunkEvent m1 = consumerTemplate.receiveBody(url, 5000, SplunkEvent.class);
        final SplunkEvent m2 = consumerTemplate.receiveBody(url, 1000, SplunkEvent.class);
        final SplunkEvent m3 = consumerTemplate.receiveBody(url, 1000, SplunkEvent.class);

        List result = Arrays.stream(new SplunkEvent[] { m1, m2, m3 })
                .map(m -> {
                    if (m == null) {
                        return "null";
                    }
                    return m.getEventData().get("_raw");
                })
                .collect(Collectors.toList());

        return result.toString();
    }

    @Path("/directRealtimePolling")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map directRealtimePolling() throws Exception {
        final SplunkEvent m1 = consumerTemplate.receiveBody("direct:realtimePolling", 3000, SplunkEvent.class);

        if (m1 == null) {
            return Collections.emptyMap();
        }

        Map result = m1.getEventData().entrySet().stream()
                .filter(e -> !e.getKey().startsWith("_"))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (v1, v2) -> v1));

        return result;
    }

    @Path("/startRealtimePolling")
    @POST
    public void startPolling(String search) {
        // use another thread for polling consumer to demonstrate that we can wait before
        // the message is sent to the queue
        Executors.newSingleThreadExecutor().execute(() -> {
            String url = String.format(
                    "splunk://realtime?username=admin&password=changeit&scheme=http&port=%d&delay=3000&initEarliestTime=rt-10s&latestTime=RAW(rt+40s)&search="
                            + search,
                    port);
            SplunkEvent body = consumerTemplate.receiveBody(url, SplunkEvent.class);
            producerTemplate.sendBody("direct:realtimePolling", body);
        });
    }

    @Path("/submit")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response submit(Map<String, String> message, @QueryParam("index") String index) throws Exception {
        return post(message, "submit", index, null);
    }

    @Path("/stream")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response stream(Map<String, String> message, @QueryParam("index") String index) throws Exception {
        return post(message, "stream", index, null);
    }

    @Path("/tcp")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response tcp(Map<String, String> message, @QueryParam("index") String index) throws Exception {
        return post(message, "tcp", index, tcpPort);
    }

    private Response post(Map<String, String> message, String endpoint, String index, Integer tcpPort) throws Exception {
        SplunkEvent se = new SplunkEvent();
        for (Map.Entry<String, String> e : message.entrySet()) {
            se.addPair(e.getKey(), e.getValue());
        }

        String url = String.format(
                "splunk:%s?scheme=http&port=%d&index=%s&sourceType=%s&source=%s",
                endpoint, port, index, SOURCE_TYPE, SOURCE);
        if (tcpPort != null) {
            url = String.format(
                    "splunk:%s?username=admin&password=changeit&scheme=http&port=%d&index=%s&sourceType=%s&source=%s&tcpReceiverLocalPort=%d&tcpReceiverPort=%d",
                    endpoint, port, index, SOURCE_TYPE, SOURCE, LOCAL_TCP_PORT, tcpPort);
        }
        final String response = producerTemplate.requestBody(url, se, String.class);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response)
                .build();
    }
}
