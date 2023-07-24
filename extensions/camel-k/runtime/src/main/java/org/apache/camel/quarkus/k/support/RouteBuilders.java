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
package org.apache.camel.quarkus.k.support;

import java.io.Reader;

import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.endpoint.EndpointRouteBuilder;
import org.apache.camel.quarkus.k.core.Source;
import org.apache.camel.util.function.ThrowingBiConsumer;
import org.apache.camel.util.function.ThrowingConsumer;

public final class RouteBuilders {

    private RouteBuilders() {
    }

    public static EndpointRouteBuilder endpoint(Source source,
            ThrowingBiConsumer<Reader, EndpointRouteBuilder, Exception> consumer) {
        return new EndpointRouteBuilder() {
            @Override
            public void configure() throws Exception {
                try (Reader reader = source.resolveAsReader(getContext())) {
                    consumer.accept(reader, this);
                }
            }
        };
    }

    public static EndpointRouteBuilder endpoint(ThrowingConsumer<EndpointRouteBuilder, Exception> consumer) {
        return new EndpointRouteBuilder() {
            @Override
            public void configure() throws Exception {
                consumer.accept(this);
            }
        };
    }

    public static RoutesBuilder route(Source source, ThrowingBiConsumer<Reader, RouteBuilder, Exception> consumer) {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                try (Reader reader = source.resolveAsReader(getContext())) {
                    consumer.accept(reader, this);
                }
            }
        };
    }

    public static RoutesBuilder route(ThrowingConsumer<RouteBuilder, Exception> consumer) {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                consumer.accept(this);
            }
        };
    }
}
