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

import io.quarkus.dev.spi.HotReplacementContext;
import io.quarkus.dev.spi.HotReplacementSetup;
import org.jboss.logging.Logger;

public class CamelHotReplacementSetup implements HotReplacementSetup {
    private static final long INITIAL_DELAY = TimeUnit.SECONDS.toMillis(5);
    private static final long TASK_DELAY = TimeUnit.SECONDS.toMillis(2);
    private static final Logger LOG = Logger.getLogger(CamelHotReplacementSetup.class);
    private Timer timer;

    @Override
    public void setupHotDeployment(HotReplacementContext context) {
        timer = new Timer("camel-live-reload", true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    context.doScan(false);
                } catch (Exception e) {
                    LOG.warn("Camel live reload task failed", e);
                }
            }
        }, INITIAL_DELAY, TASK_DELAY);
    }

    @Override
    public void close() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}
