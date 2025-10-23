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
package org.apache.camel.quarkus.component.jt400.it;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.ibm.as400.access.QueuedMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.ServiceStatus;
import org.apache.camel.component.jt400.Jt400Component;
import org.apache.camel.component.jt400.Jt400Endpoint;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/jt400")
@ApplicationScoped
public class Jt400Resource {
    private static final Logger LOGGER = LoggerFactory.getLogger(Jt400Resource.class);

    @ConfigProperty(name = "cq.jt400.url")
    String jt400Url;

    @ConfigProperty(name = "cq.jt400.username")
    String jt400Username;

    @ConfigProperty(name = "cq.jt400.password")
    String jt400Password;

    @ConfigProperty(name = "cq.jt400.keyed-queue")
    String jt400KeyedQueue;

    @ConfigProperty(name = "cq.jt400.library")
    String jt400Library;

    @ConfigProperty(name = "cq.jt400.lifo-queue")
    String jt400LifoQueue;

    @ConfigProperty(name = "cq.jt400.message-queue")
    String jt400MessageQueue;

    @ConfigProperty(name = "cq.jt400.message-replyto-queue")
    String jt400MessageReplyToQueue;

    @ConfigProperty(name = "cq.jt400.user-space")
    String jt400UserSpace;

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Inject
    CamelContext context;

    @Inject
    InquiryMessageHolder inquiryMessageHolder;

    @Path("/dataQueue/read/")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response keyedDataQueueRead(String key, @QueryParam("format") String format,
            @QueryParam("searchType") String searchType) {

        boolean keyed = key != null && !key.isEmpty();
        String _format = Optional.ofNullable(format).orElse("text");
        String _searchType = Optional.ofNullable(searchType).orElse("EQ");
        StringBuilder suffix = new StringBuilder();

        if (keyed) {
            suffix.append(jt400KeyedQueue)
                    .append(String.format("?keyed=true&format=%s&searchKey=%s&searchType=%s", _format, key, _searchType));
        } else {
            suffix.append(jt400LifoQueue).append(String.format("?readTimeout=100&format=%s", _format));
        }

        Exchange ex = consumerTemplate.receive(getUrlForLibrary(suffix.toString()));

        if ("binary".equals(format)) {
            return generateResponse(new String(ex.getIn().getBody(byte[].class), StandardCharsets.UTF_8), ex);
        }
        return generateResponse(ex.getIn().getBody(String.class), ex);

    }

    @Path("/dataQueue/write/")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response keyedDataQueueWrite(@QueryParam("key") String key,
            @QueryParam("format") String format,
            String data) {
        String _format = Optional.ofNullable(format).orElse("text");
        boolean keyed = key != null;
        StringBuilder suffix = new StringBuilder();
        Map<String, Object> headers = new HashMap<>();
        String msg;

        if (keyed) {
            suffix.append(jt400KeyedQueue).append("?keyed=true").append("&format=").append(_format);
            headers.put(Jt400Endpoint.KEY, key);
            msg = "Hello From KDQ: " + data;
        } else {
            suffix.append(jt400LifoQueue).append("?format=").append(_format);
            msg = "Hello From DQ: " + data;
        }

        Object retVal;
        if ("binary".equals(format)) {
            byte[] result = (byte[]) producerTemplate.requestBodyAndHeaders(
                    getUrlForLibrary(suffix.toString()),
                    ("Hello (bin) " + data).getBytes(StandardCharsets.UTF_8),
                    headers);
            retVal = new String(result, StandardCharsets.UTF_8);
        } else {
            retVal = producerTemplate.requestBodyAndHeaders(
                    getUrlForLibrary(suffix.toString()),
                    msg,
                    headers);
        }

        return Response.ok().entity(retVal).build();
    }

    @Path("/route/start/{route}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response startRoute(@PathParam("route") String routeName) throws Exception {
        if (context.getRouteController().getRouteStatus(routeName).isStartable()) {
            context.getRouteController().startRoute(routeName);
        }

        return Response.ok().entity(context.getRouteController().getRouteStatus(routeName).isStarted()).build();
    }

    @Path("/route/stop/{route}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response stopRoute(@PathParam("route") String routeName) throws Exception {
        LOGGER.info("Stopping route: {}", routeName);
        ServiceStatus routeStatus = context.getRouteController().getRouteStatus(routeName);
        if (context.getRouteController().getRouteStatus(routeName).isStoppable()) {
            LOGGER.info("Route {} before stop information: {}", routeName, routeStatus.toString());
            context.getRouteController().stopRoute(routeName);
            LOGGER.info("Route {} stopped", routeName);
        }
        routeStatus = context.getRouteController().getRouteStatus(routeName);
        LOGGER.info("Route {} status: {}", routeName, routeStatus.toString());

        return Response.ok().entity(routeStatus.isStopped()).build();
    }

    @Path("/component/stopWrong")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response stopComponent() throws Exception {
        Jt400Component comp = context.getComponent("jt400", Jt400Component.class);
        comp.close();
        //this second call to close connection won't wprk, because the connection pool is already closing
        //the call would need to read from a resource bundle therefore it covers existence of resource bundle in the native
        comp.getConnectionPool().close();
        return Response.ok().build();
    }

    @Path("/inquiryMessageSetExpected")
    @POST
    public void inquiryMessageSetExpected(String msg) {
        inquiryMessageHolder.setMessageText(msg);
    }

    @Path("/inquiryMessageProcessed")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String inquiryMessageProcessed() {
        return String.valueOf(inquiryMessageHolder.isProcessed());
    }

    @Path("/messageQueue/write/")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response messageQueueWrite(String data) {
        Object ex = producerTemplate.requestBody(getUrlForLibrary(jt400MessageQueue), "Hello from MQ: " + data);

        return Response.ok().entity(ex).build();
    }

    @Path("/messageQueue/read/")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response messageQueueRead(@QueryParam("queue") String queue) {
        Exchange ex = consumerTemplate
                .receive(getUrlForLibrary(queue == null ? jt400MessageQueue : queue) + "?messageAction=SAME");

        return generateResponse(ex.getIn().getBody(String.class), ex);
    }

    @Path("/programCall")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response programCall() throws Exception {
        Exchange ex = producerTemplate.request(getUrl("/qsys.lib/QUSRTVUS.PGM?fieldsLength=20,4,4,16&outputFieldsIdx=3"),
                exchange -> {
                    String userSpace = String.format("%-10s", jt400UserSpace);
                    String userLib = String.format("%-10s", jt400Library);

                    Object[] parms = new Object[] {
                            userSpace + userLib, // Qualified user space name
                            1, // starting position
                            16, // length of data
                            "" // output
                    };
                    exchange.getIn().setBody(parms);
                });

        return Response.ok().entity(ex.getIn().getBody(Object[].class)[3]).build();
    }

    private String getUrlForLibrary(String suffix) {
        return String.format("jt400://%s:%s@%s%s", jt400Username, jt400Password, jt400Url,
                "/QSYS.LIB/" + jt400Library + ".LIB/" + suffix);
    }

    private String getUrl(String suffix) {
        return String.format("jt400://%s:%s@%s%s", jt400Username, jt400Password, jt400Url, suffix);
    }

    Response generateResponse(String result, Exchange ex) {
        Map<String, Object> retVal = new HashMap<>();

        retVal.put("result", result);
        ex.getIn().getHeaders().entrySet().stream().forEach(e -> {
            if (e.getValue() instanceof QueuedMessage) {
                retVal.put(e.getKey(), "QueuedMessage: " + ((QueuedMessage) e.getValue()).getText());
            } else {
                retVal.put(e.getKey(), e.getValue());
            }
        });

        return Response.ok().entity(retVal).build();

    }
}
