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
package org.apache.camel.quarkus.component.platform.http.it;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import org.apache.camel.Component;
import org.apache.camel.Consumer;
import org.apache.camel.Endpoint;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.component.webhook.WebhookCapableEndpoint;
import org.apache.camel.component.webhook.WebhookConfiguration;
import org.apache.camel.support.DefaultComponent;
import org.apache.camel.support.DefaultEndpoint;

public class WebhookComponentProducer {

    @Produces
    @Named("webhook-delegate")
    TestWebhookComponent createWebhookComponent() {
        return new TestWebhookComponent();
    }

    static final class TestWebhookComponent extends DefaultComponent {

        @Override
        protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
            TestWebhookEndpoint endpoint = new TestWebhookEndpoint(uri, this);
            endpoint.setWebhookHandler(processor -> exchange -> {
                exchange.getMessage().setBody("Camel Quarkus Webhook");
                processor.process(exchange);
            });
            return endpoint;
        }
    }

    static final class TestWebhookEndpoint extends DefaultEndpoint implements WebhookCapableEndpoint {

        private static final List<String> DEFAULT_METHOD = Collections.unmodifiableList(Collections.singletonList("POST"));

        private Function<Processor, Processor> webhookHandler;

        private Runnable register;

        private Runnable unregister;

        private Supplier<List<String>> methods;

        private Supplier<Producer> producer;

        private Function<Processor, Consumer> consumer;

        private WebhookConfiguration webhookConfiguration;

        public TestWebhookEndpoint(String uri, Component component) {
            super(uri, component);
        }

        @Override
        public Processor createWebhookHandler(Processor next) {
            if (this.webhookHandler != null) {
                return this.webhookHandler.apply(next);
            }
            return next;
        }

        @Override
        public void registerWebhook() {
            if (this.register != null) {
                this.register.run();
            }
        }

        @Override
        public void unregisterWebhook() {
            if (this.unregister != null) {
                this.unregister.run();
            }
        }

        @Override
        public void setWebhookConfiguration(WebhookConfiguration webhookConfiguration) {
            this.webhookConfiguration = webhookConfiguration;
        }

        @Override
        public List<String> getWebhookMethods() {
            return this.methods != null ? this.methods.get() : DEFAULT_METHOD;
        }

        @Override
        public Producer createProducer() throws Exception {
            return this.producer != null ? this.producer.get() : null;
        }

        @Override
        public Consumer createConsumer(Processor processor) throws Exception {
            return this.consumer != null ? this.consumer.apply(processor) : null;
        }

        public void setWebhookHandler(Function<Processor, Processor> webhookHandler) {
            this.webhookHandler = webhookHandler;
        }
    }
}
