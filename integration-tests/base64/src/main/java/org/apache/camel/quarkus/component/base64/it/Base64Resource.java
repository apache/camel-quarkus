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
package org.apache.camel.quarkus.component.base64.it;

import java.net.URI;
import java.util.Locale;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ProducerTemplate;
import org.jboss.logging.Logger;

@Path("/base64")
@ApplicationScoped
public class Base64Resource {

    private static final Logger LOG = Logger.getLogger(Base64Resource.class);

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/post")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.TEXT_PLAIN)
    public Response post(String message) throws Exception {
        LOG.infof("Sending to base64: %s", message);
        final String response = producerTemplate.requestBody("direct:start", message, String.class);
        LOG.infof("Got response from base64: %s", response);
        LOG.warn("Default locale " + Locale.getDefault());
        for (Locale l : Locale.getAvailableLocales()) {
            LOG.warn("-- avail locale " + l);
        }
        LOG.warn("de-de " + String.format(Locale.GERMANY, "%.8f,%.8f", 0.1, 0.1));
        LOG.warn("US " + String.format(Locale.US, "%.8f,%.8f", 0.1, 0.1));
        LOG.warn("ENGLISH" + String.format(Locale.ENGLISH, "%.8f,%.8f", 0.1, 0.1));
        LOG.warn("root " + String.format(Locale.ROOT, "%.8f,%.8f", 0.1, 0.1));
        return Response
                .created(new URI("https://camel.apache.org/"))
                .header("content-length", response.length())
                .entity(response)
                .build();
    }
}
