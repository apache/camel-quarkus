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
package org.apache.camel.quarkus.component.flatpack.it;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.flatpack.DataSetList;
import org.jboss.logging.Logger;

@Path("/flatpack")
@ApplicationScoped
public class FlatpackResource {

    private static final Logger LOG = Logger.getLogger(FlatpackResource.class);

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/delimited-unmarshal")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String delimitedUnmarshal(String data) {
        LOG.infof("Invoking delimitedUnmarshal with data: %s", data);
        DataSetList unmarshalled = producerTemplate.requestBody("direct:delimited-unmarshal", data, DataSetList.class);
        return unmarshalled.size() + "-" + unmarshalled.get(0).get("ITEM_DESC");
    }

    @Path("/delimited-marshal")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String delimitedMarshal(List<Map<String, String>> object) {
        LOG.infof("Invoking delimitedMarshal with object: %s", object);
        return producerTemplate.requestBody("direct:delimited-marshal", object, String.class);
    }

    @Path("/fixed-length-unmarshal")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String fixedLengthUnmarshal(String data) {
        LOG.infof("Invoking fixedLengthUnmarshal with data: %s", data);
        DataSetList unmarshalled = producerTemplate.requestBody("direct:fixed-length-unmarshal", data, DataSetList.class);
        return unmarshalled.size() + "-" + unmarshalled.get(0).get("FIRSTNAME");
    }

    @Path("/fixed-length-marshal")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String fixedLengthMarshal(List<Map<String, String>> object) {
        LOG.infof("Invoking fixedLengthMarshal with object: %s", object);
        return producerTemplate.requestBody("direct:fixed-length-marshal", object, String.class);
    }

    @Path("/delimited")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public LinkedList<?> delimited(String data) {
        LOG.infof("Invoking delimited with data: %s", data);
        return producerTemplate.requestBody("direct:delimited", data, LinkedList.class);
    }

    @Path("/fixed")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public LinkedList<?> fixed(String data) {
        LOG.infof("Invoking fixed with data: %s", data);
        return producerTemplate.requestBody("direct:fixed", data, LinkedList.class);
    }

    @Path("/headerAndTrailer")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public LinkedList<?> headerAndTrailer(String data) {
        LOG.infof("Invoking headerAndTrailer with data: %s", data);
        return producerTemplate.requestBody("direct:header-and-trailer", data, LinkedList.class);
    }

    @Path("/noDescriptor")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public LinkedList<?> noDescriptor(String data) {
        LOG.infof("Invoking noDescriptor with data: %s", data);
        return producerTemplate.requestBody("direct:no-descriptor", data, LinkedList.class);
    }

    @Path("/invalid")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String invalid(String data) {
        LOG.infof("Invoking invalid with data: %s", data);
        return producerTemplate.requestBody("direct:fixed", data, String.class);
    }

}
