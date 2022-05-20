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
package org.apache.camel.quarkus.core.events;

import java.util.Optional;

import org.apache.camel.CamelContext;
import org.apache.camel.Route;
import org.apache.camel.Service;
import org.apache.camel.impl.event.AbstractContextEvent;
import org.apache.camel.spi.CamelEvent;

/**
 * Base {@link CamelEvent} for {@link Service} related events.
 */
public class ServiceEvent extends AbstractContextEvent {
    private final Service service;
    private final Route route;

    public ServiceEvent(CamelContext camelContext, Service service, Route route) {
        super(camelContext);
        this.service = service;
        this.route = route;
    }

    public Service getService() {
        return this.service;
    }

    public <T extends Service> T getService(Class<T> type) {
        return type.cast(this.service);
    }

    public Optional<Route> getRoute() {
        return Optional.ofNullable(route);
    }

    @Override
    public Type getType() {
        return Type.Custom;
    }
}
