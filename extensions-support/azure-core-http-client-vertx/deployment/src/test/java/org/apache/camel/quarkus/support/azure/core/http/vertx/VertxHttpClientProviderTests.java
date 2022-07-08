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

import java.net.InetSocketAddress;
import java.time.Duration;

import com.azure.core.http.ProxyOptions;
import com.azure.core.util.Configuration;
import com.azure.core.util.HttpClientOptions;
import io.quarkus.test.QuarkusUnitTest;
import io.vertx.ext.web.client.WebClientOptions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests {@link VertxHttpClientProvider}.
 */
@Disabled //https://github.com/apache/camel-quarkus/issues/4090
public class VertxHttpClientProviderTests {

    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Test
    public void nullOptionsReturnsBaseClient() {
        VertxHttpClient httpClient = (VertxHttpClient) new VertxHttpClientProvider()
                .createInstance(null);

        ProxyOptions environmentProxy = ProxyOptions.fromConfiguration(Configuration.getGlobalConfiguration());
        WebClientOptions options = httpClient.getWebClientOptions();
        io.vertx.core.net.ProxyOptions proxyOptions = options.getProxyOptions();
        if (environmentProxy == null) {
            assertNull(proxyOptions);
        } else {
            assertNotNull(proxyOptions);
            assertEquals(environmentProxy.getAddress().getHostName(), proxyOptions.getHost());
        }
    }

    @Test
    public void defaultOptionsReturnsBaseClient() {
        VertxHttpClient httpClient = (VertxHttpClient) new VertxHttpClientProvider()
                .createInstance(new HttpClientOptions());

        ProxyOptions environmentProxy = ProxyOptions.fromConfiguration(Configuration.getGlobalConfiguration());
        WebClientOptions options = httpClient.getWebClientOptions();
        io.vertx.core.net.ProxyOptions proxyOptions = options.getProxyOptions();
        if (environmentProxy == null) {
            assertNull(proxyOptions);
        } else {
            assertNotNull(proxyOptions);
            assertEquals(environmentProxy.getAddress().getHostName(), proxyOptions.getHost());
        }
    }

    @Test
    public void optionsWithAProxy() {
        ProxyOptions proxyOptions = new ProxyOptions(ProxyOptions.Type.HTTP, new InetSocketAddress("localhost", 8888));
        proxyOptions.setNonProxyHosts("foo.*|bar.*|cheese.com|wine.org");

        HttpClientOptions clientOptions = new HttpClientOptions().setProxyOptions(proxyOptions);

        VertxHttpClient httpClient = (VertxHttpClient) new VertxHttpClientProvider()
                .createInstance(clientOptions);

        WebClientOptions options = httpClient.getWebClientOptions();
        io.vertx.core.net.ProxyOptions vertxProxyOptions = options.getProxyOptions();
        assertNotNull(vertxProxyOptions);
        assertEquals(proxyOptions.getAddress().getHostName(), vertxProxyOptions.getHost());
        assertEquals(proxyOptions.getAddress().getPort(), vertxProxyOptions.getPort());
        assertEquals(proxyOptions.getType().name(), vertxProxyOptions.getType().name());
    }

    @Test
    public void optionsWithTimeouts() {
        long expectedTimeout = 15000;
        Duration timeout = Duration.ofMillis(expectedTimeout);
        HttpClientOptions clientOptions = new HttpClientOptions()
                .setWriteTimeout(timeout)
                .setResponseTimeout(timeout)
                .setReadTimeout(timeout);

        VertxHttpClient httpClient = (VertxHttpClient) new VertxHttpClientProvider()
                .createInstance(clientOptions);

        WebClientOptions options = httpClient.getWebClientOptions();

        assertEquals(timeout.getSeconds(), options.getWriteIdleTimeout());
        assertEquals(timeout.getSeconds(), options.getReadIdleTimeout());
    }
}
