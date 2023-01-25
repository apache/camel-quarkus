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

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.quartz.QuartzComponent;
import org.quartz.Scheduler;

@Path("/quartz/clustered")
public class QuartzClusteredResource {

    @Inject
    CamelContext camelContext;

    @jakarta.enterprise.inject.Produces
    @Singleton
    @Named("quartz")
    public QuartzComponent quartzComponent(Scheduler scheduler) {
        QuartzComponent component = new QuartzComponent();
        component.setScheduler(scheduler);
        return component;
    }

    @Path("/start/route")
    @POST
    public void startRoute() throws Exception {
        camelContext.addRoutes(new RouteBuilder() {
            @Override
            public void configure() {
                from("quartz:camel-quarkus/1 * * * *")
                        .log("Hello from {{quartz.node.name}}");
            }
        });
    }
}
