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
package org.apache.camel.quarkus.component.ssh.it;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/ssh")
@ApplicationScoped
public class SshResource {

    public enum ServerType {
        userPassword, user01Key, edKey
    };

    @ConfigProperty(name = "quarkus.ssh.host")
    String host;
    @ConfigProperty(name = "quarkus.ssh.port")
    String port;
    @ConfigProperty(name = "quarkus.ssh.secured-port")
    String securedPort;
    @ConfigProperty(name = "quarkus.ssh.ed-port")
    String edPort;
    @ConfigProperty(name = "ssh.username")
    String username;
    @ConfigProperty(name = "ssh.password")
    String password;

    @Inject
    CamelContext camelContext;

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @POST
    @Path("/file/{fileName}")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response writeToFile(@PathParam("fileName") String fileName, String content)
            throws URISyntaxException {

        String sshWriteFileCommand = String.format("printf \"%s\" > %s", content, fileName);
        producerTemplate.sendBody(
                String.format("ssh:%s:%s?username=%s&password=%s", host, port, username, password),
                sshWriteFileCommand);

        return Response
                .created(new URI("https://camel.apache.org/"))
                .build();
    }

    @GET
    @Path("/file/{fileName}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response readFile(@PathParam("fileName") String fileName) throws URISyntaxException {

        String sshReadFileCommand = String.format("cat %s", fileName);
        String content = consumerTemplate.receiveBody(
                String.format("ssh:%s:%s?username=%s&password=%s&pollCommand=%s", host, port, username, password,
                        sshReadFileCommand),
                String.class);

        return Response
                .ok(content)
                .build();
    }

    @POST
    @Path("/send")
    @Consumes(MediaType.APPLICATION_JSON)
    public Map<String, String> send(@QueryParam("command") String command,
            @QueryParam("component") @DefaultValue("ssh") String component,
            @QueryParam("serverType") @DefaultValue("userPassword") String serverType,
            @QueryParam("pathSuffix") String pathSuffix,
            Map<String, Object> headers)
            throws URISyntaxException {

        var port = switch (ServerType.valueOf(serverType)) {
        case userPassword -> this.port;
        case edKey -> edPort;
        case user01Key -> securedPort;
        };

        String url = String.format("%s:%s@%s:%s", component, username, host, port);
        if (pathSuffix != null) {
            url += "?" + pathSuffix;
        }
        Exchange exchange = producerTemplate.request(url,
                e -> {
                    e.getIn().setHeaders(headers == null ? Collections.emptyMap() : headers);
                    e.getIn().setBody(command == null ? "" : command);
                });

        Map<String, String> result = new HashMap<>();
        result.put("body", exchange.getMessage().getBody(String.class));
        result.putAll(exchange.getMessage().getHeaders().entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        //convert inputStreams
                        entry -> String.valueOf(entry.getValue() instanceof InputStream
                                ? camelContext.getTypeConverter().convertTo(String.class, entry.getValue())
                                : entry.getValue()))));

        return result;
    }

    @Path("/sendToDirect/{direct}")
    @POST
    public String sendToDirect(@PathParam("direct") String direct, String body) throws Exception {
        return producerTemplate.requestBody("direct:" + direct, body, String.class).trim();
    }

}
