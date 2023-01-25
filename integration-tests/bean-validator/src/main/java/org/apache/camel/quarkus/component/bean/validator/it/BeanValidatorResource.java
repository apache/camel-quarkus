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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
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

    @Inject
    ValidatorFactoryCustomizer.MyMessageInterpolator messageInterpolator;

    @Path("/get/{optional}/{manufactor}/{plate}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response get(@PathParam("optional") String endpoint, @PathParam("manufactor") String manufactor,
            @PathParam("plate") String plate) throws Exception {
        return get(new Car(manufactor, plate), endpoint);
    }

    @Path("/get/{optional}/{manufactor}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getNull(@PathParam("optional") String endpoint, @PathParam("manufactor") String manufactor)
            throws Exception {
        return get(new Car(manufactor, null), endpoint);
    }

    private Response get(Car car, String endpoint) throws Exception {
        LOG.info("bean-validator: " + car.getManufacturer() + "/" + car.getLicensePlate());
        Exchange out = producerTemplate.request("direct:" + endpoint, e -> e.getMessage().setBody(car));
        if (messageInterpolator.getCount() == 0) {
            return Response.status(500, "Interpolator was not used.").build();
        }
        if (out.isFailed()) {
            return Response.status(400, "Invalid car").build();
        } else {
            return Response.status(200, "OK").build();
        }
    }
}
