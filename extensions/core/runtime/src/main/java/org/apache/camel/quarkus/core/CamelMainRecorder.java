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

import java.io.InputStream;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.main.MainListener;
import org.apache.camel.model.Model;
import org.apache.camel.support.ResourceHelper;
import org.apache.camel.util.ObjectHelper;

@Recorder
public class CamelMainRecorder {
    public RuntimeValue<CamelMain> createCamelMain(
            RuntimeValue<CamelContext> runtime,
            BeanContainer container) {

        CamelMain main = new CamelMain();
        main.setCamelContext(runtime.getValue());
        main.disableHangupSupport();
        main.addMainListener(new CamelMainEventDispatcher());

        // register to the container
        container.instance(CamelMainProducers.class).setMain(main);

        return new RuntimeValue<>(main);
    }

    public void addRouteBuilder(
            RuntimeValue<CamelMain> main,
            String className) {

        try {
            main.getValue().addRouteBuilder(Class.forName(className));
        } catch (Exception e) {
            throw new RuntimeException("Could not add route builder '" + className + "'", e);
        }
    }


    public void addRouteBuilder(
            RuntimeValue<CamelMain> main,
            RuntimeValue<RoutesBuilder> routesBuilder) {

        RoutesBuilder builder = routesBuilder.getValue();

        // TODO: camel main may need to support RoutesBuilder instead of RouteBuilder only
        if (!(builder instanceof RouteBuilder)) {
            throw new IllegalArgumentException("Cannot handle routes builder of type: '" + builder.getClass().getName() + "'");
        }

        try {
            main.getValue().addRouteBuilder((RouteBuilder)builder);
        } catch (Exception e) {
            throw new RuntimeException("Could not add route builder '" + builder.getClass().getName() + "'", e);
        }
    }

    public void addRoutesFromLocation(
            RuntimeValue<CamelMain> main,
            String location) {

        if (ObjectHelper.isNotEmpty(location)) {
            // TODO: if pointing to a directory, we should load all xmls in it
            //   (maybe with glob support in it to be complete)
            try (InputStream is = ResourceHelper.resolveMandatoryResourceAsInputStream(main.getValue().getCamelContext(), location)) {
                main.getValue().getCamelContext().getExtension(Model.class).addRouteDefinitions(is);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void addListener(RuntimeValue<CamelMain> main, MainListener listener) {
        main.getValue().addMainListener(listener);
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
}
