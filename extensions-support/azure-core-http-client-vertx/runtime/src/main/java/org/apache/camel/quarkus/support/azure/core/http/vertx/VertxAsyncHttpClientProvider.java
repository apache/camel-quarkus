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

import com.azure.core.http.HttpClient;
import com.azure.core.http.HttpClientProvider;
import com.azure.core.util.Configuration;
import com.azure.core.util.HttpClientOptions;

/**
 * {@link HttpClientProvider} backed by the Vert.x {@link io.vertx.core.http.HttpClient}
 */
public class VertxAsyncHttpClientProvider implements HttpClientProvider {
    private static final boolean AZURE_ENABLE_HTTP_CLIENT_SHARING = Configuration.getGlobalConfiguration()
            .get("AZURE_ENABLE_HTTP_CLIENT_SHARING", Boolean.FALSE);
    private final boolean enableHttpClientSharing;

    // Enum Singleton Pattern
    private enum GlobalVertxHttpClient {
        HTTP_CLIENT(new VertxAsyncHttpClientBuilder().build());

        private final HttpClient httpClient;

        GlobalVertxHttpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
        }

        private HttpClient getHttpClient() {
            return httpClient;
        }
    }

    /**
     * For testing purpose only, assigning 'AZURE_ENABLE_HTTP_CLIENT_SHARING' to 'enableHttpClientSharing' for
     * 'final' modifier.
     */
    public VertxAsyncHttpClientProvider() {
        enableHttpClientSharing = AZURE_ENABLE_HTTP_CLIENT_SHARING;
    }

    VertxAsyncHttpClientProvider(Configuration configuration) {
        enableHttpClientSharing = configuration.get("AZURE_ENABLE_HTTP_CLIENT_SHARING", Boolean.FALSE);
    }

    @Override
    public HttpClient createInstance() {
        if (enableHttpClientSharing) {
            return GlobalVertxHttpClient.HTTP_CLIENT.getHttpClient();
        }
        return new VertxAsyncHttpClientBuilder().build();
    }

    @Override
    public HttpClient createInstance(HttpClientOptions clientOptions) {
        if (clientOptions == null) {
            return createInstance();
        }

        return new VertxAsyncHttpClientBuilder()
                .proxy(clientOptions.getProxyOptions())
                .configuration(clientOptions.getConfiguration())
                .connectTimeout(clientOptions.getConnectTimeout())
                .idleTimeout(clientOptions.getConnectionIdleTimeout())
                .writeIdleTimeout(clientOptions.getWriteTimeout())
                .readIdleTimeout(clientOptions.getReadTimeout())
                .build();
    }
}
