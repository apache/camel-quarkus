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
package org.apache.camel.quarkus.core.runtime;

import io.quarkus.arc.runtime.BeanContainerListener;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.quarkus.core.runtime.support.FastCamelRuntime;
import org.apache.camel.spi.Registry;

@Recorder
public class CamelRecorder {

    public RuntimeValue<FastCamelRuntime> create(Registry registry) {

        FastCamelRuntime fcr = new FastCamelRuntime();
        fcr.setRegistry(registry);

        return new RuntimeValue<>(fcr);
    }


    public void setBuildTimeConfig(
            RuntimeValue<FastCamelRuntime> runtime,
            CamelConfig.BuildTime buildTimeConfig) {
        runtime.getValue().setBuildTimeConfig(buildTimeConfig);
    }

    public void setRuntimeConfig(
            RuntimeValue<FastCamelRuntime> runtime,
            CamelConfig.Runtime runtimeConfig) {
        runtime.getValue().setRuntimeConfig(runtimeConfig);
    }
    public void init(
            RuntimeValue<FastCamelRuntime> runtime) {

        runtime.getValue().init();
    }

    public void start(
            ShutdownContext shutdown,
            RuntimeValue<FastCamelRuntime> runtime) throws Exception {

        runtime.getValue().start();

        //in development mode undertow is started eagerly
        shutdown.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                try {
                    runtime.getValue().stop();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public void addBuilder(
        RuntimeValue<FastCamelRuntime> runtime,
        String className) {

        FastCamelRuntime fcr = runtime.getValue();

        try {
            fcr.getBuilders().add((RoutesBuilder) Class.forName(className).newInstance());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void bind(
        RuntimeValue<FastCamelRuntime> runtime,
        String name,
        Class<?> type,
        Object instance) {

        runtime.getValue().getRegistry().bind(name, type, instance);
    }

    public void bind(
        RuntimeValue<FastCamelRuntime> runtime,
        String name,
        Class<?> type) {

        try {
            runtime.getValue().getRegistry().bind(name, type, type.newInstance());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public BeanContainerListener initRuntimeInjection(RuntimeValue<FastCamelRuntime> runtime) {
        return container -> container.instance(CamelProducers.class).setCamelRuntime(runtime.getValue());
    }
}
