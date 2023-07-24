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
package org.apache.camel.quarkus.k.support;

import java.util.Map;
import java.util.Properties;

import org.apache.camel.CamelContext;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.quarkus.k.core.Runtime;
import org.apache.camel.spi.Registry;

public class DelegatingRuntime implements Runtime {

    private final Runtime runtime;

    public DelegatingRuntime(Runtime runtime) {
        this.runtime = runtime;
    }

    @Override
    public ExtendedCamelContext getExtendedCamelContext() {
        return runtime.getExtendedCamelContext();
    }

    @Override
    public Registry getRegistry() {
        return runtime.getRegistry();
    }

    @Override
    public void setProperties(Properties properties) {
        runtime.setProperties(properties);
    }

    @Override
    public void setProperties(Map<String, String> properties) {
        runtime.setProperties(properties);
    }

    @Override
    public void setProperties(String key, String value, String... keyVals) {
        runtime.setProperties(key, value, keyVals);
    }

    @Override
    public void addRoutes(RoutesBuilder builder) {
        runtime.addRoutes(builder);
    }

    @Override
    public void stop() throws Exception {
        runtime.stop();
    }

    @Override
    public void close() throws Exception {
        runtime.close();
    }

    @Override
    public CamelContext getCamelContext() {
        return runtime.getCamelContext();
    }
}
