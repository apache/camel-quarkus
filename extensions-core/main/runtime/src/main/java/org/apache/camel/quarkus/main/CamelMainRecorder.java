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
package org.apache.camel.quarkus.main;

import java.util.List;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.main.BaseMainSupport;
import org.apache.camel.main.MainListener;
import org.apache.camel.main.MainListenerSupport;
import org.apache.camel.main.RoutesCollector;
import org.apache.camel.quarkus.core.CamelContextCustomizer;
import org.apache.camel.quarkus.core.CamelProducers;
import org.apache.camel.quarkus.core.CamelRuntime;
import org.apache.camel.quarkus.core.RegistryRoutesLoader;
import org.apache.camel.spi.XMLRoutesDefinitionLoader;

@Recorder
public class CamelMainRecorder {
    public RuntimeValue<CamelMain> createCamelMain(
            RuntimeValue<CamelContext> runtime,
            RuntimeValue<RoutesCollector> routesCollector,
            BeanContainer container) {
        CamelMain main = new CamelMain();
        main.setRoutesCollector(routesCollector.getValue());
        main.setCamelContext(runtime.getValue());
        main.addMainListener(new CamelMainEventDispatcher());

        // autowire only non null values as components may have configured
        // through CDI or from a Build Item thus those values should not be
        // overridden
        main.configure().setAutowireComponentPropertiesNonNullOnly(true);

        // properties are loaded through MicroProfile Config so there's
        // no need to look for sources.
        main.setDefaultPropertyPlaceholderLocation("false");

        // xml rest/routes should be explicitly configured as an
        // additional dependency is required thus, disable auto
        // discovery
        main.configure().setXmlRoutes("false");
        main.configure().setXmlRests("false");

        // register to the container
        container.instance(CamelMainProducers.class).setMain(main);

        return new RuntimeValue<>(main);
    }

    public void addRoutesBuilder(RuntimeValue<CamelMain> main, String className) {
        try {
            CamelContext context = main.getValue().getCamelContext();
            Class<RoutesBuilder> type = context.getClassResolver().resolveClass(className, RoutesBuilder.class);
            RoutesBuilder builder = context.getInjector().newInstance(type, false);

            main.getValue().configure().addRoutesBuilder(builder);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void addListener(RuntimeValue<CamelMain> main, RuntimeValue<MainListener> listener) {
        main.getValue().addMainListener(listener.getValue());
    }

    public RuntimeValue<RoutesCollector> newRoutesCollector(
            RuntimeValue<RegistryRoutesLoader> registryRoutesLoader,
            RuntimeValue<XMLRoutesDefinitionLoader> xmlRoutesLoader) {

        return new RuntimeValue<>(new CamelMainRoutesCollector(registryRoutesLoader.getValue(), xmlRoutesLoader.getValue()));
    }

    public void customizeContext(RuntimeValue<CamelMain> main, List<RuntimeValue<CamelContextCustomizer>> contextCustomizers) {
        main.getValue().addMainListener(new MainListenerSupport() {
            @Override
            public void afterConfigure(BaseMainSupport main) {
                for (RuntimeValue<CamelContextCustomizer> customizer : contextCustomizers) {
                    customizer.getValue().customize(main.getCamelContext());
                }
            }
        });
    }

    public RuntimeValue<CamelRuntime> createRuntime(BeanContainer beanContainer, RuntimeValue<CamelMain> main) {
        final CamelRuntime runtime = new CamelMainRuntime(main.getValue());

        // register to the container
        beanContainer.instance(CamelProducers.class).setRuntime(runtime);

        return new RuntimeValue<>(runtime);
    }
}
