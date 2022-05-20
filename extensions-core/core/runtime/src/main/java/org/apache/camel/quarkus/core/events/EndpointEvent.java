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

import org.apache.camel.Endpoint;
import org.apache.camel.impl.event.AbstractContextEvent;
import org.apache.camel.spi.CamelEvent;

/**
 * Base {@link CamelEvent} for {@link Endpoint} related events.
 */
public class EndpointEvent extends AbstractContextEvent {
    private final Endpoint endpoint;

    public EndpointEvent(Endpoint endpoint) {
        super(endpoint.getCamelContext());
        this.endpoint = endpoint;
    }

    public Endpoint getEndpoint() {
        return this.endpoint;
    }

    public <T extends Endpoint> T getEndpoint(Class<T> type) {
        return type.cast(this.endpoint);
    }

    @Override
    public Type getType() {
        return Type.Custom;
    }
}
