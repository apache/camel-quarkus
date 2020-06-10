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

import org.apache.camel.CamelContext;

/**
 * A simple implementation of the {@link CamelRuntime} that directly starts/stop the {@link CamelContext}.
 */
public class CamelContextRuntime implements CamelRuntime {
    private final CamelContext camelContext;

    public CamelContextRuntime(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    @Override
    public void start() {
        camelContext.start();
    }

    @Override
    public void stop() {
        camelContext.stop();
    }

    @Override
    public CamelContext getCamelContext() {
        return camelContext;
    }
}
