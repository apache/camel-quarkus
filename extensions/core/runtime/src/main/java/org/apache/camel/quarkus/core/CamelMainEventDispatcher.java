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
package org.apache.camel.quarkus.core;

import io.quarkus.arc.Arc;
import org.apache.camel.CamelContext;
import org.apache.camel.main.MainSupport;

/**
 * Bridges {@link MainSupport} events to CDI.
 */
public class CamelMainEventDispatcher implements org.apache.camel.main.MainListener {
    @Override
    public void beforeStart(MainSupport main) {
        fireEvent(CamelMainEvents.BeforeStart.class, new CamelMainEvents.BeforeStart());
    }

    @Override
    public void configure(CamelContext context) {
        fireEvent(CamelMainEvents.Configure.class, new CamelMainEvents.Configure());
    }

    @Override
    public void afterStart(MainSupport main) {
        fireEvent(CamelMainEvents.AfterStart.class, new CamelMainEvents.AfterStart());
    }

    @Override
    public void beforeStop(MainSupport main) {
        fireEvent(CamelMainEvents.BeforeStop.class, new CamelMainEvents.BeforeStop());
    }

    @Override
    public void afterStop(MainSupport main) {
        fireEvent(CamelMainEvents.AfterStop.class, new CamelMainEvents.AfterStop());
    }

    private static <T> void fireEvent(Class<T> clazz, T event) {
        Arc.container().beanManager().getEvent().select(clazz).fire(event);
    }
}
