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
package org.acme.health;

import java.util.Map;

import org.apache.camel.health.HealthCheckResultBuilder;
import org.apache.camel.impl.health.AbstractHealthCheck;

/**
 * A user defined health check that reports UNKNOWN on first call, then UP for
 * 10 seconds and finally DOWN afterward. This is a custom implementation of a
 * Camel {@link org.apache.camel.health.HealthCheck} and used as part of Camel's
 * health-check system.
 */
public class RunTooLongHealthCheck extends AbstractHealthCheck {

    private volatile long firstCallTimeMillis = 0;

    public RunTooLongHealthCheck() {
        super("custom", "toolong");
    }

    @Override
    protected void doCall(HealthCheckResultBuilder builder, Map<String, Object> options) {
        builder.detail("toolong", "Reports DOWN when run for too long");
        if (firstCallTimeMillis == 0) {
            builder.unknown();
            firstCallTimeMillis = System.currentTimeMillis();
        } else if ((System.currentTimeMillis() - firstCallTimeMillis) < 10 * 1000L) {
            builder.up();
        } else {
            builder.down();
        }
    }

    @Override
    public boolean isReadiness() {
        // only liveness probe
        return false;
    }
}
