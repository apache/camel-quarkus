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
package org.apache.camel.quarkus.component.bean.validator.it;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.quarkus.component.bean.validator.it.model.Car;
import org.jboss.logging.Logger;

@Path("/bean-validator")
@ApplicationScoped
public class BeanValidatorResource {

    private static final Logger LOG = Logger.getLogger(BeanValidatorResource.class);

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/get/{manufactor}/{plate}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response get(@PathParam("manufactor") String manufactor, @PathParam("plate") String plate) throws Exception {
        LOG.info("bean-validator: " + manufactor + "/" + plate);
        Car car = new Car(manufactor, plate);
        Exchange out = producerTemplate.request("direct:start", e -> e.getMessage().setBody(car));
        if (out.isFailed()) {
            return Response.status(400, "Invalid car").build();
        } else {
            return Response.status(200, "OK").build();
        }
    }

}
