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
package org.apache.camel.quarkus.component.reactive.streams;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.apache.camel.CamelContext;
import org.apache.camel.component.reactive.streams.api.CamelReactiveStreams;
import org.apache.camel.component.reactive.streams.api.CamelReactiveStreamsService;

/**
 * Producers of CamelReactiveStreams related beans that are injectable via CDI.
 */
@Singleton
public class ReactiveStreamsProducers {
    private final CamelContext camelContext;

    public ReactiveStreamsProducers(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    @Singleton
    @Produces
    CamelReactiveStreamsService camelReactiveStreamsService() {
        return CamelReactiveStreams.get(camelContext);
    }
}
