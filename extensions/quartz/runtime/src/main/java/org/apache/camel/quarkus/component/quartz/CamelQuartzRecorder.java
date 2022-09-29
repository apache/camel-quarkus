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
package org.apache.camel.quarkus.component.quartz;

import java.util.LinkedList;
import java.util.stream.Collectors;

import javax.enterprise.inject.AmbiguousResolutionException;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.quartz.QuartzScheduler;
import io.quarkus.quartz.runtime.QuartzSchedulerImpl;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.component.quartz.QuartzComponent;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Recorder
public class CamelQuartzRecorder {

    public RuntimeValue<QuartzComponent> createQuartzComponent() {
        return new RuntimeValue<>(new QuarkusQuartzComponent());
    }

    @org.apache.camel.spi.annotations.Component("quartz")
    static class QuarkusQuartzComponent extends QuartzComponent {
        private static final Logger LOG = LoggerFactory.getLogger(QuarkusQuartzComponent.class);

        @Override
        public boolean isAutowiredEnabled() {
            //autowiring is executed via custom code in doStart method
            return false;
        }

        @Override
        protected void doStartScheduler() throws Exception {
            //autowire scheduler before the start

            //if autowiring is enabled, execute it here, because special approach because of Quarkus has to be applied
            if (super.isAutowiredEnabled() && getCamelContext().isAutowiredEnabled()) {
                InjectableInstance<Scheduler> schedulers = Arc.container().select(Scheduler.class);

                LinkedList<Scheduler> foundSchedulers = new LinkedList<>();

                for (InstanceHandle<Scheduler> handle : schedulers.handles()) {
                    //Scheduler may be null in several cases, which would cause an exception in traditional autowiring
                    //see https://github.com/quarkusio/quarkus/issues/27929 for more details
                    if (handle.getBean().getBeanClass().equals(QuartzSchedulerImpl.class)) {
                        Scheduler scheduler = Arc.container().select(QuartzScheduler.class).getHandle().get().getScheduler();
                        if (scheduler != null) {
                            //scheduler is added only if is not null
                            foundSchedulers.add(scheduler);
                        }
                        continue;
                    }
                    foundSchedulers.add(handle.get());
                }

                if (foundSchedulers.size() > 1) {
                    throw new AmbiguousResolutionException(String.format("Found %d org.quartz.Scheduler beans (%s).",
                            foundSchedulers.size(), foundSchedulers.stream().map(s -> {
                                try {
                                    return s.getSchedulerName();
                                } catch (SchedulerException e) {
                                    return "Scheduler name retrieval failed.";
                                }
                            }).collect(Collectors.joining(", "))));
                } else if (!foundSchedulers.isEmpty()) {
                    if (LOG.isInfoEnabled()) {
                        LOG.info(
                                "Autowired property: scheduler on component: quartz as exactly one instance of type: {} found in the registry",
                                Scheduler.class.getName());
                    }
                    setScheduler(foundSchedulers.getFirst());
                }
            }

            super.doStartScheduler();
        }
    }
}
