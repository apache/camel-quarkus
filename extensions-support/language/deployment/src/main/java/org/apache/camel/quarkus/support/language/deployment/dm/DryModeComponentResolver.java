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
package org.apache.camel.quarkus.support.language.deployment.dm;

import java.util.Set;

import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.impl.engine.DefaultComponentResolver;

/**
 * {@code DryModeComponentResolver} is used to resolve all non-accepted components with {@link DryModeComponent} for a
 * dry run.
 * The accepted components are safe to start and stop for a dry run and cannot be replaced with a
 * {@link DryModeComponent}.
 */
class DryModeComponentResolver extends DefaultComponentResolver {

    /**
     * Name of components for which a mock component is not needed for the dry run.
     */
    private static final Set<String> ACCEPTED_NAMES = Set.of("bean", "class", "kamelet");

    @Override
    public Component resolveComponent(String name, CamelContext context) {
        if (ACCEPTED_NAMES.contains(name)) {
            return super.resolveComponent(name, context);
        }
        return new DryModeComponent();
    }
}
