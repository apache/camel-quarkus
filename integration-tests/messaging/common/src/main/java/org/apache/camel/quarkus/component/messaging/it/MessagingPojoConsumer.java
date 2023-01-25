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
package org.apache.camel.quarkus.component.messaging.it;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.camel.Consume;
import org.apache.camel.quarkus.component.messaging.it.util.scheme.ComponentScheme;

@Singleton
public class MessagingPojoConsumer {

    private static BlockingQueue<String> messages = new LinkedBlockingQueue<>();

    @Inject
    ComponentScheme scheme;

    public String getMessagingUri() {
        return scheme + ":queue:pojoConsume";
    }

    @Consume(property = "messagingUri")
    public void consumeMessage(String content) {
        try {
            messages.put("Hello " + content);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public String getMessage(long timeout) {
        try {
            return messages.poll(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return null;
    }
}
