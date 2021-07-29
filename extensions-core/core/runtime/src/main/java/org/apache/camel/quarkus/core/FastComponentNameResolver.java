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

import java.util.Set;

import org.apache.camel.CamelContext;
import org.apache.camel.spi.ComponentNameResolver;

/**
 * A fast {@link ComponentNameResolver} implementation, returning a fixed set of component names that were
 * discovered at build time
 */
public class FastComponentNameResolver implements ComponentNameResolver {

    private final Set<String> componentNames;

    public FastComponentNameResolver(Set<String> componentNames) {
        this.componentNames = componentNames;
    }

    @Override
    public Set<String> resolveNames(CamelContext camelContext) {
        return componentNames;
    }
}
