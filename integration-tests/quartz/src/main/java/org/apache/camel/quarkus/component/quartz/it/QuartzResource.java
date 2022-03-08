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
package org.apache.camel.quarkus.component.quartz.it;

import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.component.quartz.QuartzComponent;
import org.apache.camel.util.CollectionHelper;
import org.quartz.CronTrigger;

@Path("/quartz")
public class QuartzResource {

    @Inject
    CamelContext camelContext;

    @Inject
    ConsumerTemplate consumerTemplate;

    @javax.enterprise.inject.Produces
    @Singleton
    @Named("quartzFromProperties")
    public QuartzComponent createQuartzFromProperties() {
        return new QuartzComponent();
    }

    @javax.enterprise.inject.Produces
    @Singleton
    @Named("quartzNodeA")
    public QuartzComponent createQuartzNodeA() {
        return new QuartzComponent();
    }

    @javax.enterprise.inject.Produces
    @Singleton
    @Named("quartzNodeB")
    public QuartzComponent createQuartzNodeB() {
        return new QuartzComponent();
    }

    @javax.enterprise.inject.Produces
    @Singleton
    @Named("quartzNodeC")
    public QuartzComponent createQuartzNodeC() {
        return new QuartzComponent();
    }

    @Path("/getNameAndResult")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> getSchedulerNameAndResult(@QueryParam("componentName") String componentName,
            @QueryParam("fromEndpoint") String fromEndpoint) throws Exception {

        QuartzComponent comp = camelContext.getComponent(componentName, QuartzComponent.class);

        return CollectionHelper.mapOf("name", comp.getScheduler().getSchedulerName().replaceFirst(camelContext.getName(), ""),
                "result", consumerTemplate.receiveBody("seda:" + fromEndpoint + "-result", 5000, String.class));
    }

    @Path("/get")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getSchedulerResult(@QueryParam("fromEndpoint") String fromEndpoint) throws Exception {
        return consumerTemplate.receiveBody("seda:" + fromEndpoint + "-result", 5000, String.class);
    }

    @Path("/getHeaders")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> getHeaders(@QueryParam("fromEndpoint") String fromEndpoint) throws Exception {
        Exchange exchange = consumerTemplate.receive("seda:" + fromEndpoint + "-result", 5000);

        return exchange.getMessage().getHeaders().entrySet().stream().filter(e -> e.getValue() instanceof String)
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));
    }

    @Path("/getMisfire")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> getMisfire(@QueryParam("fromEndpoint") String fromEndpoint) throws Exception {
        Exchange exchange = consumerTemplate.receive("seda:" + fromEndpoint + "-result", 5000);

        System.out.println(exchange.getMessage().getHeaders().keySet().stream().collect(Collectors.joining(",")));
        return CollectionHelper.mapOf("timezone",
                exchange.getMessage().getHeader("trigger", CronTrigger.class).getTimeZone().getID(),
                "misfire", exchange.getMessage().getHeader("trigger", CronTrigger.class).getMisfireInstruction() + "");
    }
}
