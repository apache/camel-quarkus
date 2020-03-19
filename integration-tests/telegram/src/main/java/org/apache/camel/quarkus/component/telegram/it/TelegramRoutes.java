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

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.support.ResourceHelper;
import org.apache.camel.util.IOHelper;

public class TelegramRoutes extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        /* Mock Telegram API */
        from("platform-http:/bot{authToken}/getUpdates?httpMethodRestrict=GET")
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
                    from("platform-http:/{authToken}/" + endpoint + "?httpMethodRestrict=POST")
                            .process(e -> load("mock-messages/" + endpoint + ".json", e));
                });

    }

    private static void load(String path, Exchange exchange) {
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
