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
package org.apache.camel.quarkus.component.quartz.deployment;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.quartz.QuartzComponent;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class QuartzQuarkusSchedulerAutowiredWithSchedulerBeanTest {

    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .setLogRecordPredicate(logRecord -> logRecord.getMessage().contains("Scheduled Job"))
            .assertLogRecords(logRecords -> {
                boolean quarkusScheduledJobRan = logRecords.stream()
                        .anyMatch(logRecord -> logRecord.getMessage().startsWith("Quarkus Scheduled"));
                boolean camelScheduledJobRan = logRecords.stream()
                        .anyMatch(logRecord -> logRecord.getMessage().startsWith("Camel Scheduled"));
                assertTrue(quarkusScheduledJobRan);
                assertTrue(camelScheduledJobRan);
            });

    @Inject
    CamelContext context;

    @Test
    public void testQuarkusSchedulerAutowired() throws Exception {
        QuartzComponent component = context.getComponent("quartz", QuartzComponent.class);
        assertEquals("QuarkusQuartzScheduler", component.getScheduler().getSchedulerName());
    }

    @ApplicationScoped
    static final class SchedulerBean {
        private static final Logger LOG = Logger.getLogger(SchedulerBean.class);

        @Scheduled(every = "1s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
        public void scheduledQuarkusJob() {
            LOG.info("Quarkus Scheduled Job");
        }
    }

    @ApplicationScoped
    static final class Routes extends RouteBuilder {
        private static final Logger LOG = Logger.getLogger(Routes.class);

        @Override
        public void configure() throws Exception {
            from("quartz://scheduledCamelJob?cron=0/1+*+*+*+*+?")
                    .process(exchange -> {
                        LOG.info("Camel Scheduled Job");
                    });
        }
    }
}
