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
import org.apache.camel.builder.RouteBuilder;

@ApplicationScoped
public class SchedulerRoute extends RouteBuilder {

    @Inject
    @Named("schedulerCounter")
    AtomicInteger schedulerCounter;

    @Inject
    @Named("withDelayCounter")
    AtomicInteger withDelayCounter;

    @Inject
    @Named("useFixedDelayCounter")
    AtomicInteger useFixedDelayCounter;

    @Inject
    @Named("withDelayRepeatCounter")
    AtomicInteger withDelayRepeatCounter;

    @Inject
    @Named("greedyCounter")
    AtomicInteger greedyCounter;

    @Override
    public void configure() throws Exception {
        from("scheduler:withInitialDelay?initialDelay=1").routeId("withInitialDelay").noAutoStartup()
                .process(e -> schedulerCounter.incrementAndGet());

        from("scheduler:withDelay?delay=100").routeId("withDelay").noAutoStartup()
                .process(e -> withDelayCounter.incrementAndGet());

        from("scheduler:useFixedDelay?initialDelay=200&useFixedDelay=true").routeId("useFixedDelay").noAutoStartup()
                .process(e -> useFixedDelayCounter.incrementAndGet());

        from("scheduler:withDelayRepeat?delay=1&repeatCount=5").routeId("withDelayRepeat").noAutoStartup()
                .process(e -> withDelayRepeatCounter.incrementAndGet());

        from("scheduler:greedy?delay=100&greedy=true").routeId("greedy").noAutoStartup()
                .process(e -> greedyCounter.incrementAndGet());
    }
}
