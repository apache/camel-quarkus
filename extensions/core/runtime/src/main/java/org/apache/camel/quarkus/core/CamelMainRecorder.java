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

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.impl.engine.DefaultReactiveExecutor;
import org.apache.camel.main.MainListener;
import org.apache.camel.main.RoutesCollector;
import org.apache.camel.spi.ReactiveExecutor;

@Recorder
public class CamelMainRecorder {
    public RuntimeValue<ReactiveExecutor> createReactiveExecutor() {
        return new RuntimeValue<>(new DefaultReactiveExecutor());
    }

    public RuntimeValue<CamelMain> createCamelMain(
            RuntimeValue<CamelContext> runtime,
            RuntimeValue<RoutesCollector> routesCollector,
            BeanContainer container) {

        CamelMain main = new CamelMain();
        main.setRoutesCollector(routesCollector.getValue());
        main.setCamelContext(runtime.getValue());
        main.addMainListener(new CamelMainEventDispatcher());

        // xml rest/routes should be explicitly configured as an
        // additional dependency is required thus, disable auto
        // discovery
        main.configure().setXmlRoutes("false");
        main.configure().setXmlRests("false");

        // register to the container
        container.instance(CamelMainProducers.class).setMain(main);

        return new RuntimeValue<>(main);
    }

    public void addRouteBuilder(
            RuntimeValue<CamelMain> main,
            Class<? extends RoutesBuilder> routeBuilderClass) {

        try {
            main.getValue().addRouteBuilder(routeBuilderClass);
        } catch (Exception e) {
            throw new RuntimeException("Could not add route builder '" + routeBuilderClass.getName() + "'", e);
        }
    }

    public void addListener(RuntimeValue<CamelMain> main, RuntimeValue<MainListener> listener) {
        main.getValue().addMainListener(listener.getValue());
    }

    public void setReactiveExecutor(RuntimeValue<CamelMain> main, RuntimeValue<ReactiveExecutor> executor) {
        main.getValue().getCamelContext().setReactiveExecutor(executor.getValue());
    }

    public void start(ShutdownContext shutdown, RuntimeValue<CamelMain> main) {
        shutdown.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                try {
                    main.getValue().stop();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });

        try {
            main.getValue().init();
            main.getValue().start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public RuntimeValue<RoutesCollector> newDisabledXmlRoutesCollector() {
        return new RuntimeValue<>(new DisabledXmlRoutesCollector());
    }
}
