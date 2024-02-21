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

import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.apache.camel.model.FromDefinition;
import org.apache.camel.model.RouteDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A bean to hold routes related logic.
 */
@ApplicationScoped
public class ApplicationRoutes {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationRoutes.class);

    @Inject
    ApplicationRoutesConfig config;

    @Inject
    CamelContext context;

    public ApplicationRoutesConfig config() {
        return this.config;
    }

    public void override(RouteDefinition definition) {
        if (config.overrides().isEmpty()) {
            return;
        }

        final String id = definition.getRouteId();
        final FromDefinition from = definition.getInput();

        for (ApplicationRoutesConfig.RouteOverride override : config.overrides().get()) {
            if (override.id().isEmpty() && override.input().from().isEmpty()) {
                continue;
            }
            if (override.id().isPresent() && !Objects.equals(override.id().get(), id)) {
                continue;
            }
            if (override.input().from().isPresent() && !Objects.equals(from.getEndpointUri(), override.input().from().get())) {
                continue;
            }

            LOGGER.debug("Replace '{}' --> '{}' for route {}",
                    from.getEndpointUri(),
                    override.input().with(),
                    definition.getRouteId());

            from.setUri(override.input().with());

            break;
        }
    }
}
