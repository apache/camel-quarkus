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
package org.apache.camel.quarkus.component.csv.it;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.ProducerTemplate;
import org.jboss.logging.Logger;

@Path("/csv")
@ApplicationScoped
public class CsvResource {

    private static final Logger LOG = Logger.getLogger(CsvResource.class);

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/json-to-csv")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String json2csv(String json) throws Exception {
        LOG.infof("Transforming json %s", json);
        final List<Map<String, Object>> objects = new ObjectMapper().readValue(json,
                new TypeReference<List<Map<String, Object>>>() {
                });
        return producerTemplate.requestBody(
                "direct:json-to-csv",
                objects,
                String.class);
    }

    @SuppressWarnings("unchecked")
    @Path("/csv-to-json")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public List<List<Object>> csv2json(String csv) throws Exception {
        return producerTemplate.requestBody("direct:csv-to-json", csv, List.class);
    }
}
