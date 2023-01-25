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
package org.apache.camel.quarkus.component.scheduler.it;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.CamelContext;

@Path("/scheduler")
@ApplicationScoped
public class SchedulerResource {

    @Inject
    CamelContext context;

    @Inject
    @Named("withDelayCounter")
    AtomicInteger withDelayCounter;

    @Inject
    @Named("useFixedDelayCounter")
    AtomicInteger useFixedDelayCounter;

    @Named("withDelayRepeatCounter")
    AtomicInteger withDelayRepeatCounter;

    @Inject
    @Named("greedyCounter")
    AtomicInteger greedyCounter;

    @Inject
    @Named("schedulerCounter")
    AtomicInteger schedulerCounter;

    @Path("/get")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public int get() throws Exception {
        return schedulerCounter.get();
    }

    @Path("/get-delay-count")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public int getCountDelay() {
        return withDelayCounter.get();
    }

    @Path("/get-fixed-delay-count")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public int getCountFixedDelay() {
        return useFixedDelayCounter.get();
    }

    @Path("/get-repeat-count")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public int getRepeatCount() {
        return withDelayRepeatCounter.get();
    }

    @Path("/get-greedy-count")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public int getGreedyCount() {
        return greedyCounter.get();
    }

    @jakarta.enterprise.inject.Produces
    @ApplicationScoped
    @Named("schedulerCounter")
    AtomicInteger schedulerCounter() {
        return new AtomicInteger();
    }

    @jakarta.enterprise.inject.Produces
    @ApplicationScoped
    @Named("withDelayRepeatCounter")
    AtomicInteger withDelayRepeatCounter() {
        return new AtomicInteger();
    }

    @jakarta.enterprise.inject.Produces
    @ApplicationScoped
    @Named("withDelayCounter")
    AtomicInteger withDelayCounter() {
        return new AtomicInteger();
    }

    @jakarta.enterprise.inject.Produces
    @ApplicationScoped
    @Named("useFixedDelayCounter")
    AtomicInteger useFixedDelayCounter() {
        return new AtomicInteger();
    }

    @jakarta.enterprise.inject.Produces
    @ApplicationScoped
    @Named("greedyCounter")
    AtomicInteger greedyCounter() {
        return new AtomicInteger();
    }

    @POST
    @Path("route/{route}/{enable}")
    public void mangeRoutes(@PathParam("route") String route, @PathParam("enable") boolean enable) throws Exception {
        if (enable) {
            context.getRouteController().startRoute(route);
        } else {
            context.getRouteController().stopRoute(route);
        }
    }

}
