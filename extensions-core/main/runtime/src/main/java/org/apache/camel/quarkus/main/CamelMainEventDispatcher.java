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

import javax.enterprise.inject.spi.BeanManager;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import org.apache.camel.CamelContext;
import org.apache.camel.main.BaseMainSupport;
import org.apache.camel.main.MainSupport;

/**
 * Bridges {@link MainSupport} events to CDI.
 */
public class CamelMainEventDispatcher implements org.apache.camel.main.MainListener {
    @Override
    public void beforeInitialize(BaseMainSupport main) {
        fireEvent(CamelMainEvents.BeforeInitialize.class, new CamelMainEvents.BeforeInitialize());
    }

    @Override
    public void beforeConfigure(BaseMainSupport main) {
        fireEvent(CamelMainEvents.BeforeConfigure.class, new CamelMainEvents.BeforeConfigure());
    }

    @Override
    public void afterConfigure(BaseMainSupport main) {
        fireEvent(CamelMainEvents.AfterConfigure.class, new CamelMainEvents.AfterConfigure());
    }

    @Override
    public void configure(CamelContext context) {
        // deprecated, replaced by afterConfigure()
    }

    @Override
    public void beforeStart(BaseMainSupport main) {
        fireEvent(CamelMainEvents.BeforeStart.class, new CamelMainEvents.BeforeStart());
    }

    @Override
    public void afterStart(BaseMainSupport main) {
        fireEvent(CamelMainEvents.AfterStart.class, new CamelMainEvents.AfterStart());
    }

    @Override
    public void beforeStop(BaseMainSupport main) {
        fireEvent(CamelMainEvents.BeforeStop.class, new CamelMainEvents.BeforeStop());
    }

    @Override
    public void afterStop(BaseMainSupport main) {
        fireEvent(CamelMainEvents.AfterStop.class, new CamelMainEvents.AfterStop());
    }

    private static <T> void fireEvent(Class<T> clazz, T event) {
        ArcContainer container = Arc.container();
        if (container != null) {
            BeanManager beanManager = container.beanManager();
            if (beanManager != null) {
                beanManager.getEvent().select(clazz).fire(event);
            }
        }
    }
}
