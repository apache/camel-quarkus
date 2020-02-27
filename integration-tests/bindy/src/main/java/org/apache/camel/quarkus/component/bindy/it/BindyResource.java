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
package org.apache.camel.quarkus.component.bindy.it;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.quarkus.component.bindy.it.model.CsvOrder;
import org.apache.camel.quarkus.component.bindy.it.model.FixedLengthOrder;
import org.apache.camel.quarkus.component.bindy.it.model.Header;
import org.apache.camel.quarkus.component.bindy.it.model.MessageOrder;
import org.apache.camel.quarkus.component.bindy.it.model.Security;
import org.apache.camel.quarkus.component.bindy.it.model.Trailer;
import org.jboss.logging.Logger;

@Path("/bindy")
@ApplicationScoped
public class BindyResource {

    private static final Logger LOG = Logger.getLogger(BindyResource.class);

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/jsonToCsv")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String jsonToCsv(final CsvOrder order) {
        LOG.infof("Invoking  jsonToCsv: %s", order);
        return producerTemplate.requestBody("direct:jsonToCsv", order, String.class);
    }

    @Path("/csvToJson")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public CsvOrder csvToJson(final String csvOrder) {
        LOG.infof("Invoking  csvToJson: %s", csvOrder);
        return producerTemplate.requestBody("direct:csvToJson", csvOrder, CsvOrder.class);
    }

    @Path("/jsonToFixedLength")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String jsonToFixedLength(final FixedLengthOrder order) {
        LOG.infof("Invoking  jsonToFixedLength: %s", order);
        return producerTemplate.requestBody("direct:jsonToFixedLength", order, String.class);
    }

    @Path("/fixedLengthToJson")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public FixedLengthOrder fixedLengthToJson(final String fixedLengthOrder) {
        LOG.infof("Invoking  fixedLengthToJson: %s", fixedLengthOrder);
        return producerTemplate.requestBody("direct:fixedLengthToJson", fixedLengthOrder, FixedLengthOrder.class);
    }

    @Path("/jsonToMessage")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String jsonToMessage(final MessageOrder order) {
        LOG.infof("Invoking  jsonToMessage: %s", order);
        Map<String, Object> model = new HashMap<>();
        model.put(MessageOrder.class.getName(), order);
        model.put(Header.class.getName(), order.getHeader());
        model.put(Trailer.class.getName(), order.getTrailer());
        model.put(Security.class.getName(), order.getSecurities().get(0));
        return producerTemplate.requestBody("direct:jsonToMessage", Arrays.asList(model), String.class);
    }

    @Path("/messageToJson")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public MessageOrder messageToJson(final String messageOrder) {
        LOG.infof("Invoking  messageToJson: %s", messageOrder);
        return producerTemplate.requestBody("direct:messageToJson", messageOrder, MessageOrder.class);
    }
}
