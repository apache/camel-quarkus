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
package org.apache.camel.quarkus.main.events;

import org.apache.camel.CamelContext;
import org.apache.camel.main.BaseMainSupport;
import org.apache.camel.spi.CamelEvent;
import org.apache.camel.spi.HasCamelContext;

/**
 * A base {@link CamelEvent} for {@link BaseMainSupport} events.
 */
public class MainEvent implements CamelEvent, HasCamelContext {
    private final BaseMainSupport main;
    private long timestamp;

    public MainEvent(BaseMainSupport main) {
        this.main = main;
    }

    public BaseMainSupport getMain() {
        return this.main;
    }

    @Override
    public CamelContext getCamelContext() {
        return getMain().getCamelContext();
    }

    public <T extends CamelContext> T getCamelContext(Class<T> type) {
        return type.cast(getMain().getCamelContext());
    }

    @Override
    public Type getType() {
        return Type.Custom;
    }

    @Override
    public Object getSource() {
        return getMain();
    }

    @Override
    public long getTimestamp() {
        return this.timestamp;
    }

    @Override
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
