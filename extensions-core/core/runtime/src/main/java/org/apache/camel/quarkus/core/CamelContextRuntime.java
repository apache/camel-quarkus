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
package org.apache.camel.quarkus.core;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import org.apache.camel.CamelContext;
import org.apache.camel.spi.CamelEvent;
import org.apache.camel.support.EventNotifierSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple implementation of the {@link CamelRuntime} that directly starts/stop the {@link CamelContext}.
 */
public class CamelContextRuntime implements CamelRuntime {
    private static final Logger LOGGER = LoggerFactory.getLogger(CamelContextRuntime.class);
    private final CamelContext camelContext;
    private final CountDownLatch latch;

    public CamelContextRuntime(CamelContext camelContext) {
        this.camelContext = camelContext;
        this.latch = new CountDownLatch(1);
    }

    @Override
    public void start(String[] args) {
        if (args.length > 0) {
            LOGGER.info("Ignoring args: {}", Arrays.toString(args));
        }
        camelContext.getManagementStrategy().addEventNotifier(new EventNotifierSupport() {
            @Override
            public void notify(CamelEvent event) throws Exception {
                latch.countDown();
            }

            @Override
            public boolean isEnabled(CamelEvent event) {
                return event instanceof CamelEvent.CamelContextStoppedEvent;
            }
        });
        camelContext.start();
    }

    @Override
    public void stop() {
        camelContext.stop();
    }

    @Override
    public int waitForExit() {
        try {
            this.latch.await();
        } catch (InterruptedException e) {
            LOGGER.warn("", e);
        }

        return 0;
    }

    @Override
    public CamelContext getCamelContext() {
        return camelContext;
    }
}
