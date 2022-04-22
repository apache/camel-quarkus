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

import java.util.Set;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;

import com.azure.core.http.HttpClient;
import com.azure.core.http.HttpClientProvider;
import com.azure.core.util.HttpClientOptions;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;

/**
 * {@link HttpClientProvider} backed by the Vert.x {@link WebClient}
 */
public class VertxHttpClientProvider implements HttpClientProvider {

    @Override
    public HttpClient createInstance() {
        return createInstance(null);
    }

    @Override
    public HttpClient createInstance(HttpClientOptions clientOptions) {
        VertxHttpClientBuilder builder = new VertxHttpClientBuilder(getVertx());
        if (clientOptions != null) {
            builder = builder.proxy(clientOptions.getProxyOptions())
                    .configuration(clientOptions.getConfiguration())
                    .connectTimeout(clientOptions.getConnectTimeout())
                    .idleTimeout(clientOptions.getConnectionIdleTimeout())
                    .writeIdleTimeout(clientOptions.getWriteTimeout())
                    .readIdleTimeout(clientOptions.getReadTimeout());
        }
        return builder.build();
    }

    /**
     * Obtains a reference to the Quarkus managed {@link Vertx} instance
     * 
     * @return The Quarkus managed {@link Vertx} instance
     */
    private static final Vertx getVertx() {
        BeanManager beanManager = CDI.current().getBeanManager();
        Set<Bean<?>> beans = beanManager.getBeans(Vertx.class);
        if (beans.isEmpty()) {
            throw new IllegalStateException("Failed to discover Vert.x bean from the CDI bean manager");
        }

        if (beans.size() > 1) {
            throw new IllegalStateException(
                    "Expected 1 Vert.x bean in the CDI bean manager but " + beans.size() + " were found");
        }

        Bean<?> bean = beanManager.resolve(beans);
        Object reference = beanManager.getReference(bean, Vertx.class, beanManager.createCreationalContext(bean));
        return (Vertx) reference;
    }
}
