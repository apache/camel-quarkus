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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.builder.RouteBuilder;

/**
 * Beans active only under the {@code quartz-scheduler-bean} build profile,
 * used by {@link org.apache.camel.quarkus.component.quartz.it.QuartzQuarkusSchedulerAutowiredWithSchedulerBeanTest}.
 */
@ApplicationScoped
@IfBuildProfile("quartz-scheduler-bean")
public class QuartzSchedulerBeanProfile {

    static final CountDownLatch QUARKUS_LATCH = new CountDownLatch(1);
    static final CountDownLatch CAMEL_LATCH = new CountDownLatch(1);

    @Scheduled(every = "1s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void scheduledQuarkusJob() {
        QUARKUS_LATCH.countDown();
    }

    @Singleton
    @IfBuildProfile("quartz-scheduler-bean")
    public static class Routes extends RouteBuilder {
        @Override
        public void configure() {
            from("quartz://schedulerBeanCamelJob?cron=0/1+*+*+*+*+?")
                    .process(exchange -> CAMEL_LATCH.countDown());
        }
    }

    @Path("/quartz/scheduler-bean")
    public static class Resource {
        @GET
        @Path("/fired")
        @Produces(MediaType.TEXT_PLAIN)
        public boolean fired() throws InterruptedException {
            return QUARKUS_LATCH.await(10, TimeUnit.SECONDS)
                    && CAMEL_LATCH.await(10, TimeUnit.SECONDS);
        }
    }
}
