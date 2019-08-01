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

import java.util.List;
import java.util.Properties;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.arc.runtime.BeanContainerListener;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.quarkus.core.runtime.support.FastCamelRuntime;
import org.apache.camel.spi.Registry;

@Recorder
public class CamelRecorder {

    public RuntimeValue<CamelRuntime> create(
            Registry registry,
            Properties properties,
            List<RuntimeValue<?>> builders,
            CamelConfig.BuildTime buildTimeConfig) {

        FastCamelRuntime runtime = new FastCamelRuntime();

        runtime.setRegistry(registry);
        runtime.setProperties(properties);
        builders.stream()
                .map(RuntimeValue::getValue)
                .map(RoutesBuilder.class::cast)
                .forEach(runtime.getBuilders()::add);

        return new RuntimeValue<>(runtime);
    }

    public void init(
            BeanContainer beanContainer,
            RuntimeValue<CamelRuntime> runtime,
            List<String> builders,
            CamelConfig.BuildTime buildTimeConfig) throws Exception {

        FastCamelRuntime fcr = (FastCamelRuntime) runtime.getValue();
        fcr.setBeanContainer(beanContainer);

        builders.stream()
                .forEach(name -> {
                    try {
                        fcr.getBuilders().add((RoutesBuilder) Class.forName(name).newInstance());
                    } catch (Throwable t) {
                        throw new RuntimeException(t);
                    }
                });

        fcr.init(buildTimeConfig);
    }

    public void start(
            ShutdownContext shutdown,
            RuntimeValue<CamelRuntime> runtime,
            CamelConfig.Runtime runtimeConfig) throws Exception {

        runtime.getValue().start(runtimeConfig);

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

    public BeanContainerListener initRuntimeInjection(RuntimeValue<CamelRuntime> runtime) {
        return container -> container.instance(CamelProducers.class).setCamelRuntime(runtime.getValue());
    }

}
