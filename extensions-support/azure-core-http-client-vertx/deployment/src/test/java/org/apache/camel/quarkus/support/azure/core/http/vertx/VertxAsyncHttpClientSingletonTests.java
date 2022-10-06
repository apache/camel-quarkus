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
import com.azure.core.test.utils.TestConfigurationSource;
import com.azure.core.util.Configuration;
import com.azure.core.util.ConfigurationBuilder;
import com.azure.core.util.ConfigurationSource;
import com.azure.core.util.HttpClientOptions;
import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@Execution(ExecutionMode.SAME_THREAD)
public class VertxAsyncHttpClientSingletonTests {
    private static final ConfigurationSource EMPTY_SOURCE = new TestConfigurationSource();

    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(VertxAsyncHttpClientProvider.class));

    @Test
    public void testSingletonClientInstanceCreation() {
        Configuration configuration = getConfiguration(true);
        HttpClient client1 = new VertxAsyncHttpClientProvider(configuration).createInstance();
        HttpClient client2 = new VertxAsyncHttpClientProvider(configuration).createInstance();
        assertEquals(client1, client2);
    }

    @Test
    public void testNonDefaultClientInstanceCreation() {
        Configuration configuration = getConfiguration(false);
        HttpClient client1 = new VertxAsyncHttpClientProvider(configuration).createInstance();
        HttpClient client2 = new VertxAsyncHttpClientProvider(configuration).createInstance();
        assertNotEquals(client1, client2);
    }

    @Test
    public void testCustomizedClientInstanceCreationNotShared() {
        Configuration configuration = getConfiguration(false);
        HttpClientOptions clientOptions = new HttpClientOptions().setMaximumConnectionPoolSize(500);
        HttpClient client1 = new VertxAsyncHttpClientProvider(configuration).createInstance(clientOptions);
        HttpClient client2 = new VertxAsyncHttpClientProvider(configuration).createInstance(clientOptions);
        assertNotEquals(client1, client2);
    }

    @Test
    public void testNullHttpClientOptionsInstanceCreation() {
        Configuration configuration = getConfiguration(true);
        HttpClient client1 = new VertxAsyncHttpClientProvider(configuration).createInstance(null);
        HttpClient client2 = new VertxAsyncHttpClientProvider(configuration).createInstance(null);
        assertEquals(client1, client2);
    }

    private static Configuration getConfiguration(boolean enableSharing) {
        return new ConfigurationBuilder(EMPTY_SOURCE, EMPTY_SOURCE, new TestConfigurationSource()
                .put("AZURE_ENABLE_HTTP_CLIENT_SHARING", Boolean.toString(enableSharing)))
                        .build();
    }
}
