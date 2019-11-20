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
package org.apache.camel.quarkus.component.json.path.it;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.ProducerTemplate;
import org.jboss.logging.Logger;

@Path("/jsonpath")
@ApplicationScoped
public class JsonPathResource {

    private static final Logger LOG = Logger.getLogger(JsonPathResource.class);

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/getBookPriceLevel")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String getBookPriceLevel(String storeRequestJson) {
        LOG.infof("Getting book price level from json store request: %s", storeRequestJson);
        return producerTemplate.requestBody("direct:getBookPriceLevel", storeRequestJson, String.class);
    }

    @Path("/getBookPrice")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String getBookPrice(String storeRequestJson) {
        LOG.infof("Getting book price from json store request: %s", storeRequestJson);
        return producerTemplate.requestBody("direct:getBookPrice", storeRequestJson, String.class);
    }

    @Path("/getFullName")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String getFullName(String personRequestJson) {
        LOG.infof("Getting person full name from json person request: %s", personRequestJson);
        return producerTemplate.requestBody("direct:getFullName", personRequestJson, String.class);
    }

    @Path("/getAllCarColors")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String getAllCarColors(String carsRequestJson) {
        LOG.infof("Getting all car colors from json cars request: %s", carsRequestJson);
        return producerTemplate.requestBody("direct:getAllCarColors", carsRequestJson, String.class);
    }
}
