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
package org.apache.camel.quarkus.component.gson.it;

import java.util.Arrays;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.quarkus.component.gson.it.model.AdvancedOrder;
import org.apache.camel.quarkus.component.gson.it.model.Order;
import org.jboss.logging.Logger;

@Path("/gson")
@ApplicationScoped
public class GsonResource {

    private static final Logger LOG = Logger.getLogger(GsonResource.class);

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/gsonMarshal")
    @GET
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.TEXT_PLAIN)
    public String gsonMarshal(Order order) {
        LOG.infof("Invoking gsonMarshal with : %s", order);
        return producerTemplate.requestBody("direct:gsonMarshal", order, String.class);
    }

    @Path("/gsonUnmarshal")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_XML)
    public Order gsonUnmarshal(String order) {
        LOG.infof("Invoking gsonUnmarshal with : %s", order);
        return producerTemplate.requestBody("direct:gsonUnmarshal", order, Order.class);
    }

    @Path("/gsonMarshalAdvanced")
    @GET
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.TEXT_PLAIN)
    public String gsonMarshalAdvanced(AdvancedOrder order) {
        LOG.infof("Invoking gsonMarshalAdvanced with : %s", order);
        return producerTemplate.requestBody("direct:gsonMarshalAdvanced", order, String.class);
    }

    @Path("/gsonUnmarshalAdvanced")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_XML)
    public AdvancedOrder gsonUnmarshalAdvanced(String order) {
        LOG.infof("Invoking gsonUnmarshalAdvanced with : %s", order);
        return producerTemplate.requestBody("direct:gsonUnmarshalAdvanced", order, AdvancedOrder.class);
    }

    @Path("/gsonMarshalGenericType")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String gsonMarshalGenericType() {
        LOG.infof("Invoking gsonMarshalGenericType");
        Order first = new Order();
        first.setReference("first");
        Order second = new Order();
        second.setReference("second");
        return producerTemplate.requestBody("direct:gsonMarshalGenericType", Arrays.asList(first, second), String.class);
    }

    @Path("/gsonUnmarshalGenericType")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String gsonUnmarshalGenericType(String json) {
        LOG.infof("Invoking gsonUnmarshalGenericType with : %s", json);
        List<?> orders = producerTemplate.requestBody("direct:gsonUnmarshalGenericType", json, List.class);
        return orders.toString();
    }
}
