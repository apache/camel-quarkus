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
package org.apache.camel.quarkus.component.microprofile.it.health;

import java.util.Map;
import java.util.stream.Stream;

import org.apache.camel.health.HealthCheck;
import org.apache.camel.health.HealthCheckRepository;
import org.apache.camel.health.HealthCheckResultBuilder;
import org.apache.camel.impl.health.AbstractHealthCheck;

/**
 * Fictitious HealthCheckRepository returning a hard coded check to verify auto discovery is working
 */
public class CustomHealthCheckRepository implements HealthCheckRepository {

    private final HealthCheck check = new AlwaysUpHealthCheck();

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void setEnabled(boolean enabled) {
        // Noop
    }

    @Override
    public Stream<HealthCheck> stream() {
        return Stream.of(check);
    }

    @Override
    public String getId() {
        return "custom-health-repo";
    }

    static final class AlwaysUpHealthCheck extends AbstractHealthCheck {

        protected AlwaysUpHealthCheck() {
            super("custom", "always-up");
        }

        @Override
        protected void doCall(HealthCheckResultBuilder builder, Map<String, Object> options) {
            builder.up().build();
        }
    }
}
