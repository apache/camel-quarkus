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
package org.apache.camel.quarkus.core.annotations;

import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.EndpointInject;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;

@Path("/core/annotations")
@ApplicationScoped
public class CoreAnnotationsResource {

    @Inject
    ProducerTemplate producerTemplate;

    @EndpointInject("direct:endpointInjectTemplate")
    ProducerTemplate endpointInjectTemplateProducer;

    @EndpointInject("direct:endpointInjectFluentTemplate")
    FluentProducerTemplate endpointInjectFluentTemplateProducer;

    @Produce("direct:produceProducer")
    ProducerTemplate produceProducer;

    @Produce("direct:produceProducerFluent")
    FluentProducerTemplate produceProducerFluent;

    @Inject
    @Named("results")
    Map<String, List<String>> results;

    @Path("/endpointInjectTemplate")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String endpointInjectTemplate(String payload) {
        endpointInjectTemplateProducer.sendBody("Sent to an @EndpointInject: " + payload);
        return awaitFirst("endpointInjectTemplate");
    }

    @Path("/endpointInjectFluentTemplate")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String endpointInjectFluentTemplate(String payload) {
        endpointInjectFluentTemplateProducer
                .withBody("Sent to an @EndpointInject fluent: " + payload)
                .send();
        return awaitFirst("endpointInjectFluentTemplate");
    }

    @Path("/endpointInjectDirect/{index}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String endpointInjectDirect(String payload, @PathParam("index") String index) {
        producerTemplate.sendBody("direct:endpointInjectDirectStart" + index, payload);
        return awaitFirst("endpointInjectDirect" + index);
    }

    @Path("/produceProducer")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String produceProducer(String payload) {
        produceProducer.sendBody("Sent to an @Produce: " + payload);
        return awaitFirst("produceProducer");
    }

    @Path("/produceProducerFluent")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String produceProducerFluent(String payload) {
        produceProducerFluent
                .withBody("Sent to an @Produce fluent: " + payload)
                .send();
        return awaitFirst("produceProducerFluent");
    }

    String awaitFirst(String key) {
        final List<String> list = results.get(key);
        final long timeout = System.currentTimeMillis() + 10000;
        do {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        } while (list.isEmpty() && System.currentTimeMillis() < timeout);
        return list.get(0);
    }

}
