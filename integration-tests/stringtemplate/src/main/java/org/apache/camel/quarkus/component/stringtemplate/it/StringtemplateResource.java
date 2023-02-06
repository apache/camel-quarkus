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
package org.apache.camel.quarkus.component.stringtemplate.it;

import java.net.URI;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.jboss.logging.Logger;

@Path("/stringtemplate")
@ApplicationScoped
public class StringtemplateResource {

    private static final Logger LOG = Logger.getLogger(StringtemplateResource.class);

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/template")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response post(Map<String, Object> headers,
            @QueryParam("template") String template,
            @QueryParam("body") String body,
            @QueryParam("parameters") String parameters) throws Exception {
        LOG.infof("Sending to stringtemplate: %s", headers);
        String endpointUri = "string-template:" + template + (parameters != null ? "?" + parameters : "");
        final Exchange response = producerTemplate.request(endpointUri, exchange -> {
            exchange.getIn().setBody(body);
            exchange.getIn().setHeaders(headers);
        });
        LOG.infof("Got response from stringtemplate: %s", response.getMessage().getBody());
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response.getMessage().getBody())
                .build();
    }
}
