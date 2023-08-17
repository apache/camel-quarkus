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
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.splunk.ProducerType;
import org.apache.camel.component.splunk.SplunkComponent;
import org.apache.camel.component.splunk.SplunkConfiguration;
import org.apache.camel.component.splunk.event.SplunkEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/splunk")
@ApplicationScoped
public class SplunkResource {

    public static final String SAVED_SEARCH_NAME = "savedSearchForTest";
    public static final String PARAM_REMOTE_PORT = "org.apache.camel.quarkus.component.splunk.it.SplunkResource_remotePort";
    public static final String PARAM_TCP_PORT = "org.apache.camel.quarkus.component.splunk.it.SplunkResource_tcpPort";
    public static final String SOURCE = "test";
    public static final int LOCAL_TCP_PORT = 9998;

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @ConfigProperty(name = PARAM_REMOTE_PORT)
    Integer port;

    @ConfigProperty(name = PARAM_TCP_PORT)
    Integer tcpPort;

    @Named
    SplunkComponent splunk() {
        SplunkComponent component = new SplunkComponent();
        component.setSplunkConfigurationFactory(parameters -> new SplunkConfiguration());
        return component;
    }

    @Path("/results/{name}")
    @POST
    public String results(@PathParam("name") String mapName) throws Exception {
        String url;
        int count = 3;

        if ("savedSearch".equals(mapName)) {
            url = String.format(
                    "splunk://savedsearch?username=admin&password=changeit&scheme=http&port=%d&delay=500&initEarliestTime=-10m&savedsearch=%s",
                    port, SAVED_SEARCH_NAME);
        } else if ("normalSearch".equals(mapName)) {
            url = String.format(
                    "splunk://normal?username=admin&password=changeit&scheme=http&port=%d&delay=5000&initEarliestTime=-10s&search="
                            + "search sourcetype=\"SUBMIT\" | rex field=_raw \"Name: (?<name>.*) From: (?<from>.*)\"",
                    port);
        } else {
            url = String.format(
                    "splunk://realtime?username=admin&password=changeit&scheme=http&port=%d&delay=3000&initEarliestTime=rt-10s&latestTime=RAW(rt+40s)&search="
                            + "search sourcetype=\"STREAM\" | rex field=_raw \"Name: (?<name>.*) From: (?<from>.*)\"",
                    port, ProducerType.STREAM.name());
        }

        List<SplunkEvent> events = events = new LinkedList<>();
        for (int i = 0; i < count; i++) {
            SplunkEvent se = consumerTemplate.receiveBody(url, 5000, SplunkEvent.class);
            if (se == null) {
                break;
            }
            events.add(se);
        }
        List result = events.stream()
                .map(m -> {
                    if (m == null) {
                        return "null";
                    }
                    return m.getEventData().get("_raw");
                })
                .collect(Collectors.toList());
        return result.toString();
    }

    @Path("/write/{producerType}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response write(Map<String, String> message,
            @PathParam("producerType") String producerType,
            @QueryParam("index") String index) throws URISyntaxException {
        if (message.containsKey("_rawData")) {
            return writeRaw(message.get("_rawData"), producerType, index);
        }

        SplunkEvent se = new SplunkEvent();
        for (Map.Entry<String, String> e : message.entrySet()) {
            se.addPair(e.getKey(), e.getValue());
        }

        return writeRaw(se, producerType, index);
    }

    private Response writeRaw(Object message,
            String producerType,
            String index) throws URISyntaxException {
        String url;
        if (ProducerType.TCP == ProducerType.valueOf(producerType)) {
            url = String.format(
                    "splunk:%s?raw=%b&username=admin&password=changeit&scheme=http&port=%d&index=%s&sourceType=%s&source=%s&tcpReceiverLocalPort=%d&tcpReceiverPort=%d",
                    producerType.toLowerCase(), !(message instanceof SplunkEvent), port, index, producerType, SOURCE,
                    LOCAL_TCP_PORT, tcpPort);

        } else {
            url = String.format(
                    "splunk:%s?raw=%b&scheme=http&port=%d&index=%s&sourceType=%s&source=%s",
                    producerType.toLowerCase(), !(message instanceof SplunkEvent), port, index, producerType, SOURCE);
        }
        final String response = producerTemplate.requestBody(url, message, String.class);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response)
                .build();
    }
}
