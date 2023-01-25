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
package org.apache.camel.quarkus.main;

import java.util.Set;
import java.util.function.Supplier;

import io.quarkus.arc.Arc;
import jakarta.enterprise.inject.spi.BeanManager;
import org.apache.camel.CamelContext;
import org.apache.camel.main.BaseMainSupport;
import org.apache.camel.main.MainListener;
import org.apache.camel.main.MainSupport;
import org.apache.camel.quarkus.main.events.AfterConfigure;
import org.apache.camel.quarkus.main.events.AfterStart;
import org.apache.camel.quarkus.main.events.AfterStop;
import org.apache.camel.quarkus.main.events.BeforeConfigure;
import org.apache.camel.quarkus.main.events.BeforeInitialize;
import org.apache.camel.quarkus.main.events.BeforeStart;
import org.apache.camel.quarkus.main.events.BeforeStop;
import org.apache.camel.quarkus.main.events.MainEvent;
import org.apache.camel.util.function.Suppliers;

/**
 * Bridges {@link MainSupport} events to CDI.
 */
public class CamelMainEventBridge implements MainListener {
    private final Supplier<BeanManager> beanManager;
    private final Set<String> observedMainEvents;

    public CamelMainEventBridge(Set<String> observedMainEvents) {
        this.beanManager = Suppliers.memorize(Arc.container()::beanManager);
        this.observedMainEvents = observedMainEvents;
    }

    @Override
    public void beforeInitialize(BaseMainSupport main) {
        if (observedMainEvents.contains(BeforeInitialize.class.getName())) {
            fireEvent(new BeforeInitialize(main));
        }
    }

    @Override
    public void beforeConfigure(BaseMainSupport main) {
        if (observedMainEvents.contains(BeforeConfigure.class.getName())) {
            fireEvent(new BeforeConfigure(main));
        }
    }

    @Override
    public void afterConfigure(BaseMainSupport main) {
        if (observedMainEvents.contains(AfterConfigure.class.getName())) {
            fireEvent(new AfterConfigure(main));
        }
    }

    @Override
    public void configure(CamelContext context) {
        // deprecated, replaced by afterConfigure()
    }

    @Override
    public void beforeStart(BaseMainSupport main) {
        if (observedMainEvents.contains(BeforeStart.class.getName())) {
            fireEvent(new BeforeStart(main));
        }
    }

    @Override
    public void afterStart(BaseMainSupport main) {
        if (observedMainEvents.contains(AfterStart.class.getName())) {
            fireEvent(new AfterStart(main));
        }
    }

    @Override
    public void beforeStop(BaseMainSupport main) {
        if (observedMainEvents.contains(BeforeStop.class.getName())) {
            fireEvent(new BeforeStop(main));
        }
    }

    @Override
    public void afterStop(BaseMainSupport main) {
        if (observedMainEvents.contains(AfterStop.class.getName())) {
            fireEvent(new AfterStop(main));
        }
    }

    private <T extends MainEvent> void fireEvent(T event) {
        beanManager.get().getEvent().select(MainEvent.class).fire(event);
    }
}
