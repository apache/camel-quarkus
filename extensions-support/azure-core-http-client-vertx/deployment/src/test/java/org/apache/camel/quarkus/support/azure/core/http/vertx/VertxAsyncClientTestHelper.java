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
package org.apache.camel.quarkus.support.azure.core.http.vertx;

import java.lang.reflect.Field;
import java.util.function.Predicate;

import io.vertx.core.http.impl.HttpClientImpl;
import io.vertx.core.net.SocketAddress;

/**
 * Utility class to reflectively retrieve configuration settings from the Vert.x HTTP Client that are
 * not exposed by default.
 *
 * Avoids having to implement workarounds in the client code to make them available just for testing purposes.
 */
final class VertxAsyncClientTestHelper {

    private VertxAsyncClientTestHelper() {
        // Utility class
    }

    @SuppressWarnings("unchecked")
    static Predicate<SocketAddress> getVertxInternalProxyFilter(HttpClientImpl client) {
        try {
            Field field = HttpClientImpl.class.getDeclaredField("proxyFilter");
            field.setAccessible(true);
            return (Predicate<SocketAddress>) field.get(client);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
