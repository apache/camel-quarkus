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
package org.apache.camel.quarkus.dsl.groovy;

import java.util.concurrent.TimeUnit;

import org.apache.camel.dsl.groovy.GroovyRoutesBuilderLoader;
import org.apache.camel.quarkus.test.support.process.QuarkusProcessExecutor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zeroturnaround.exec.StartedProcess;

import static org.assertj.core.api.Assertions.assertThat;

class GroovyDslTest {

    private static int port;
    private static StartedProcess process;

    @BeforeAll
    static void start() throws Exception {
        // Need to use an external process to test the extension because of a CL issue that happens only on test mode
        // due to the fact that groovy is defined as a parent first artifact
        QuarkusProcessExecutor quarkusProcessExecutor = new QuarkusProcessExecutor();
        process = quarkusProcessExecutor.start();
        port = quarkusProcessExecutor.getHttpPort();
        awaitStartup();
    }

    @AfterAll
    static void stop() {
        if (process != null && process.getProcess().isAlive()) {
            process.getProcess().destroy();
        }
    }

    private static String toAbsolutePath(String relativePath) {
        return String.format("http://localhost:%d/%s", port, relativePath);
    }

    private static void awaitStartup() {
        Awaitility.await().atMost(10, TimeUnit.SECONDS).pollDelay(1, TimeUnit.SECONDS).until(() -> {
            HttpUriRequest request = new HttpGet(toAbsolutePath("/groovy-dsl"));
            try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
                HttpResponse httpResponse = client.execute(request);
                return httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
            } catch (Exception e) {
                return false;
            }
        });
    }

    @Test
    void groovyHello() throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            // Given
            HttpPost httpPost = new HttpPost(toAbsolutePath("/groovy-dsl/hello"));
            httpPost.setEntity(new StringEntity("John Smith", ContentType.TEXT_PLAIN));

            // When
            HttpResponse httpResponse = client.execute(httpPost);

            // Then
            assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
            assertThat(EntityUtils.toString(httpResponse.getEntity())).isEqualTo("Hello John Smith from Groovy!");
        }
    }

    @Test
    void testMainInstanceWithJavaRoutes() throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            // Given
            HttpUriRequest request = new HttpGet(toAbsolutePath("/groovy-dsl/main/groovyRoutesBuilderLoader"));

            // When
            HttpResponse httpResponse = client.execute(request);

            // Then
            assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
            assertThat(EntityUtils.toString(httpResponse.getEntity())).isEqualTo(GroovyRoutesBuilderLoader.class.getName());

            // Given
            request = new HttpGet(toAbsolutePath("/groovy-dsl/main/routeBuilders"));

            // When
            httpResponse = client.execute(request);

            // Then
            assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
            assertThat(EntityUtils.toString(httpResponse.getEntity())).isEmpty();

            // Given
            request = new HttpGet(toAbsolutePath("/groovy-dsl/main/routes"));

            // When
            httpResponse = client.execute(request);

            // Then
            assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
            assertThat(EntityUtils.toString(httpResponse.getEntity())).isEqualTo(
                    "my-groovy-route,routes-with-components-configuration,routes-with-dataformats-configuration,routes-with-eip-body,routes-with-eip-exchange,routes-with-eip-message,routes-with-eip-process,routes-with-eip-setBody,routes-with-endpoint-dsl,routes-with-error-handler,routes-with-languages-configuration,routes-with-rest,routes-with-rest-dsl-get,routes-with-rest-dsl-post,routes-with-rest-get,routes-with-rest-post");

            // Given
            request = new HttpGet(toAbsolutePath("/groovy-dsl/main/successful/routes"));

            // When
            httpResponse = client.execute(request);

            // Then
            assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
            assertThat(EntityUtils.toString(httpResponse.getEntity())).isEqualTo("10");
        }
    }

    @Test
    void testRestEndpoints() throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            // Given
            final HttpGet httpGet = new HttpGet(toAbsolutePath("/root/my/path/get"));

            // When
            HttpResponse httpResponse = client.execute(httpGet);

            // Then
            assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
            assertThat(EntityUtils.toString(httpResponse.getEntity())).isEqualTo("Hello World");

            // Given
            HttpPost httpPost = new HttpPost(toAbsolutePath("/root/post"));
            httpPost.setEntity(new StringEntity("Will", ContentType.TEXT_PLAIN));

            // When
            httpResponse = client.execute(httpPost);

            // Then
            assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
            assertThat(EntityUtils.toString(httpResponse.getEntity())).isEqualTo("Hello Will");
        }
    }
}
