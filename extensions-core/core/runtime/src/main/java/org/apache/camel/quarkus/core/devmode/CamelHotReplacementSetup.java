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
package org.apache.camel.quarkus.core.devmode;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import io.quarkus.dev.spi.HotReplacementContext;
import io.quarkus.dev.spi.HotReplacementSetup;
import org.jboss.logging.Logger;

/**
 * Camel hot replacement setup for Quarkus dev mode.
 * <p>
 * Provides periodic scanning for Camel-specific files that may not be detected by Quarkus's
 * built-in file watchers. To avoid conflicts with concurrent restarts, this implementation
 * tracks restart lifecycle events and skips scans while a restart is in progress.
 * <p>
 * See: https://github.com/apache/camel-quarkus/issues/8318
 */
public class CamelHotReplacementSetup implements HotReplacementSetup {
    private static final long INITIAL_DELAY = TimeUnit.SECONDS.toMillis(5);
    private static final long TASK_DELAY = TimeUnit.SECONDS.toMillis(2);
    private static final Logger LOG = Logger.getLogger(CamelHotReplacementSetup.class);

    private volatile Timer timer;
    private volatile boolean closed = false;
    private final AtomicLong preRestartTime = new AtomicLong(0);
    private final AtomicLong postRestartTime = new AtomicLong(0);

    @Override
    public void setupHotDeployment(HotReplacementContext context) {
        LOG.debugf("Setting up Camel hot deployment - initial delay: %dms, task delay: %dms",
                INITIAL_DELAY, TASK_DELAY);

        // Track when any restarts begin
        context.addPreRestartStep(() -> {
            preRestartTime.set(System.currentTimeMillis());
            LOG.trace("Restart starting - recording timestamp");
        });

        // Track when any restart completes
        context.addPostRestartStep(() -> {
            postRestartTime.set(System.currentTimeMillis());
            LOG.trace("Restart completed - recording timestamp");
        });

        timer = new Timer("camel-live-reload", true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (closed) {
                    LOG.trace("Skipping scan - HotReplacementSetup is closed");
                    return;
                }

                // Skip if a restart is currently in progress
                // (preRestartTime > postRestartTime means restart hasn't completed yet)
                if (preRestartTime.get() > postRestartTime.get()) {
                    LOG.trace("Skipping scan - restart currently in progress");
                    return;
                }

                try {
                    LOG.trace("Starting Camel live reload scan");
                    context.doScan(false);
                } catch (Exception e) {
                    LOG.warn("Camel live reload task failed", e);
                }
            }
        }, INITIAL_DELAY, TASK_DELAY);

        LOG.debug("Camel hot deployment timer scheduled successfully");
    }

    @Override
    public void close() {
        LOG.debug("Closing Camel hot replacement setup");
        closed = true;

        if (timer != null) {
            timer.cancel();
            timer = null;
            LOG.debug("Canceled Camel live reload timer");
        }
    }
}
