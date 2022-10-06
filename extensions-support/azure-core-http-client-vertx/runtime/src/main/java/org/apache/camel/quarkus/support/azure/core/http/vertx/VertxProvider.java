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

import io.vertx.core.Vertx;

/**
 * Service provider interface providing platforms and applications the means to have their own managed
 * {@link Vertx} be resolved by the {@link VertxAsyncHttpClientBuilder}.
 */
public interface VertxProvider {

    /**
     * Creates a {@link Vertx}. Could either be the result of returning {@code Vertx.vertx()},
     * or returning a {@link Vertx} that was resolved from a dependency injection framework like Spring or CDI.
     *
     * @return The created {@link Vertx}.
     */
    Vertx createVertx();
}
