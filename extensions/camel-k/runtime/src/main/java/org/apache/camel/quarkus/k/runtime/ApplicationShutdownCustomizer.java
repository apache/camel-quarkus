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
package org.apache.camel.quarkus.k.runtime;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.quarkus.runtime.Quarkus;
import org.apache.camel.CamelContext;
import org.apache.camel.spi.CamelContextCustomizer;
import org.apache.camel.spi.CamelEvent;
import org.apache.camel.support.EventNotifierSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationShutdownCustomizer implements CamelContextCustomizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationShutdownCustomizer.class);

    private final ApplicationShutdownConfig config;

    public ApplicationShutdownCustomizer(ApplicationShutdownConfig config) {
        this.config = config;
    }

    @Override
    public void configure(CamelContext camelContext) {
        if (this.config.maxMessages() > 0) {
            LOGGER.info("Configure the JVM to terminate after {} messages and none inflight", this.config.maxMessages());
            camelContext.getManagementStrategy().addEventNotifier(new ShutdownEventHandler(camelContext, config));
        }
    }

    private static final class ShutdownEventHandler extends EventNotifierSupport {
        private static final Logger LOGGER = LoggerFactory.getLogger(ShutdownEventHandler.class);

        private final CamelContext context;
        private final ApplicationShutdownConfig config;
        private final AtomicInteger counter;
        private final AtomicBoolean shutdownStarted;

        ShutdownEventHandler(CamelContext context, ApplicationShutdownConfig config) {
            this.context = context;
            this.config = config;
            this.counter = new AtomicInteger();
            this.shutdownStarted = new AtomicBoolean();

        }

        @Override
        public void notify(CamelEvent event) throws Exception {
            final int currentCounter = this.counter.incrementAndGet();
            final int currentInflight = context.getInflightRepository().size();

            LOGGER.debug("CamelEvent received (max: {}, handled: {}, inflight: {})",
                    this.config.maxMessages(),
                    currentCounter,
                    currentInflight);

            if (currentCounter < this.config.maxMessages() || currentInflight != 0) {
                return;
            }

            if (!this.shutdownStarted.compareAndExchange(false, true)) {
                LOGGER.info("Initiate runtime shutdown (max: {}, handled: {})",
                        this.config.maxMessages(),
                        currentCounter);

                Quarkus.asyncExit();
            }
        }

        @Override
        public boolean isEnabled(CamelEvent event) {
            return (event instanceof CamelEvent.ExchangeCompletedEvent || event instanceof CamelEvent.ExchangeFailedEvent);
        }
    }
}
