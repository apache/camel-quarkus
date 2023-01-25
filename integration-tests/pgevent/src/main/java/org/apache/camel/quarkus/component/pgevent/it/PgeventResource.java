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
package org.apache.camel.quarkus.component.pgevent.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ProducerTemplate;
import org.jboss.logging.Logger;

@Path("/pgevent")
@ApplicationScoped
public class PgeventResource {

    private static final Logger LOG = Logger.getLogger(PgeventResource.class);

    @Inject
    ProducerTemplate producerTemplate;

    @GET
    @Path("{event}")
    public Response publishEvent(@PathParam("event") String event) {
        LOG.infof("publish event %s", event);
        String response = producerTemplate.requestBody("direct:pgevent-pub", event, String.class);
        LOG.infof("message received : %s", response);
        return Response.accepted(response).build();
    }

    @GET
    @Path("datasource/{event}")
    public Response publishEventWithDatasource(@PathParam("event") String event) {
        LOG.infof("publish event %s", event);
        String response = producerTemplate.requestBody("direct:pgevent-datasource", event, String.class);
        LOG.infof("message received : %s", response);
        return Response.accepted(response).build();
    }

}
