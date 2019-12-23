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
import java.util.Arrays;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.util.IOHelper;

public class TelegramRoutes extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        /* Mock Telegram API */
        from("platform-http:/bot{authToken}/getUpdates?httpMethodRestrict=GET")
                .process(new ResourceSupplier("mock-messages/getUpdates.json"));
        Arrays.asList(
                "sendMessage",
                "sendAudio",
                "sendVideo",
                "sendDocument",
                "sendPhoto",
                "sendVenue",
                "sendLocation",
                "stopMessageLiveLocation").stream()
                .forEach(endpoint -> {
                    from("platform-http:/{authToken}/" + endpoint + "?httpMethodRestrict=POST")
                            .process(new ResourceSupplier("mock-messages/" + endpoint + ".json"));
                });

    }

    static class ResourceSupplier implements Processor {
        private final byte[] bytes;

        public ResourceSupplier(String path) {
            try (ByteArrayOutputStream out = new ByteArrayOutputStream(IOHelper.DEFAULT_BUFFER_SIZE);
                    InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
                IOHelper.copy(in, out, IOHelper.DEFAULT_BUFFER_SIZE);
                this.bytes = out.toByteArray();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void process(Exchange exchange) throws Exception {
            final Message m = exchange.getMessage();
            m.setBody(bytes);
            m.setHeader("Content-Length", bytes.length);
            m.setHeader("Content-Type", "application/json; charset=UTF-8");
        }

    }

}
