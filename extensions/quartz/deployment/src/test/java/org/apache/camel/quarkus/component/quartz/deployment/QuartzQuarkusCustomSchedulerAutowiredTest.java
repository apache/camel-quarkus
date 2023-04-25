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

import java.util.Properties;

import io.quarkus.test.QuarkusUnitTest;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.apache.camel.component.quartz.QuartzComponent;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class QuartzQuarkusCustomSchedulerAutowiredTest {

    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Inject
    CamelContext context;

    @Test
    public void testCustomSchedulerAutowired() throws Exception {
        QuartzComponent component = context.getComponent("quartz", QuartzComponent.class);
        assertEquals("customScheduler", component.getScheduler().getSchedulerName());
    }

    @Produces
    public Scheduler produceScheduler() throws SchedulerException {
        Properties prop = new Properties();
        prop.setProperty("org.quartz.scheduler.instanceName", "customScheduler");
        prop.setProperty("org.quartz.threadPool.threadCount", "2");
        return new StdSchedulerFactory(prop).getScheduler();
    }
}
