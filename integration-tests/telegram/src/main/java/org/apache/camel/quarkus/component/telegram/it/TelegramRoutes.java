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
package org.apache.camel.quarkus.component.telegram.it;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;

import io.quarkus.arc.Unremovable;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.telegram.TelegramComponent;
import org.apache.camel.quarkus.test.mock.backend.MockBackendUtils;
import org.apache.camel.support.ResourceHelper;
import org.apache.camel.util.IOHelper;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class TelegramRoutes extends RouteBuilder {

    @ConfigProperty(name = "camel.quarkus.start-mock-backend", defaultValue = "true")
    boolean startMockBackend;

    @ConfigProperty(name = "telegram.authorization-token", defaultValue = "default-dummy-token")
    String authToken;

    @ConfigProperty(name = "quarkus.http.test-port")
    int httpTestPort;
    @ConfigProperty(name = "quarkus.http.port")
    int httpPort;

    private String getBaseUri() {
        final boolean isNativeMode = "executable".equals(System.getProperty("org.graalvm.nativeimage.kind"));
        return "default-dummy-token".equals(authToken)
                ? "http://localhost:" + (isNativeMode ? httpPort : httpTestPort)
                : "https://api.telegram.org";
    }

    /**
     * We need to implement some conditional configuration of the {@link TelegramComponent} thus we create it
     * programmatically and publish via CDI.
     *
     * @return a configured {@link TelegramComponent}
     */
    @Produces
    @ApplicationScoped
    @Unremovable
    @Named
    TelegramComponent telegram() {
        final TelegramComponent result = new TelegramComponent();
        result.setCamelContext(getContext());
        result.setBaseUri(getBaseUri());
        result.setAuthorizationToken(authToken);
        return result;
    }

    @Override
    public void configure() throws Exception {
        if (startMockBackend) {
            MockBackendUtils.logMockBackendUsed("telegram", getBaseUri());
            /* Start the mock Telegram API unless the user did export CAMEL_QUARKUS_FALLBACK_MOCK=false */
            from("platform-http:/bot" + authToken + "/getUpdates?httpMethodRestrict=GET")
                    .process(e -> load("mock-messages/getUpdates.json", e));

            Stream.of(
                    "sendMessage",
                    "sendAudio",
                    "sendVideo",
                    "sendDocument",
                    "sendPhoto",
                    "sendVenue",
                    "sendLocation",
                    "stopMessageLiveLocation")
                    .forEach(endpoint -> {
                        from("platform-http:/bot" + authToken + "/" + endpoint + "?httpMethodRestrict=POST")
                                .process(e -> load("mock-messages/" + endpoint + ".json", e));
                    });
        } else {
            MockBackendUtils.logRealBackendUsed("telegram", getBaseUri());
        }

    }

    private void load(String path, Exchange exchange) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(IOHelper.DEFAULT_BUFFER_SIZE);
                InputStream in = ResourceHelper.resolveMandatoryResourceAsInputStream(exchange.getContext(), path)) {
            IOHelper.copy(in, out, IOHelper.DEFAULT_BUFFER_SIZE);

            final byte[] bytes = out.toByteArray();
            exchange.getMessage().setBody(bytes);
            exchange.getMessage().setHeader("Content-Length", bytes.length);
            exchange.getMessage().setHeader("Content-Type", "application/json; charset=UTF-8");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
