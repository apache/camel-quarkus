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
package org.apache.camel.quarkus.component.aws2.ses.it;

import java.net.URI;
import java.util.Collections;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.component.aws2.ses.Ses2Constants;
import org.jboss.logging.Logger;

@Path("/aws2-ses")
@ApplicationScoped
public class Aws2SesResource {
    private static final Logger LOG = Logger.getLogger(Aws2SesResource.class);

    @Inject
    FluentProducerTemplate producerTemplate;

    @Path("/send")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response post(
            String message,
            @HeaderParam("x-from") String from,
            @HeaderParam("x-to") String to,
            @HeaderParam("x-subject") String subject,
            @HeaderParam("x-returnPath") String returnPath) throws Exception {
        Object response = producerTemplate
                .to("aws2-ses:" + from)
                .withHeader(Ses2Constants.TO, Collections.singletonList(to))
                .withHeader(Ses2Constants.SUBJECT, subject)
                .withHeader(Ses2Constants.RETURN_PATH, returnPath)
                .withBody(message)
                .request();

        LOG.debugf("Message sent: %s", response);

        return Response
                .created(new URI("https://camel.apache.org/"))
                .build();
    }
}
