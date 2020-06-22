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
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.camel.CamelContext;

/**
 * @see org.apache.camel.spi.LifecycleStrategy#onThreadPoolAdd(CamelContext, ThreadPoolExecutor, String, String, String,
 *      String)
 */
public class ThreadPoolAddEvent extends ThreadPoolEvent {
    private final String id;
    private final String sourceId;
    private final String routeId;
    private final String threadPoolProfileId;

    public ThreadPoolAddEvent(CamelContext camelContext, ThreadPoolExecutor threadPool, String id, String sourceId,
            String routeId, String threadPoolProfileId) {
        super(camelContext, threadPool);
        this.id = id;
        this.sourceId = sourceId;
        this.routeId = routeId;
        this.threadPoolProfileId = threadPoolProfileId;
    }

    public Optional<String> getId() {
        return Optional.ofNullable(id);
    }

    public Optional<String> getSourceId() {
        return Optional.ofNullable(sourceId);
    }

    public Optional<String> getRouteId() {
        return Optional.ofNullable(routeId);
    }

    public Optional<String> getThreadPoolProfileId() {
        return Optional.ofNullable(threadPoolProfileId);
    }
}
