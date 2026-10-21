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
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.main.BaseMainSupport;
import org.apache.camel.main.MainListener;
import org.apache.camel.main.MainListenerSupport;
import org.apache.camel.quarkus.core.CamelConfig.FailureRemedy;
import org.apache.camel.quarkus.core.CamelProducers;
import org.apache.camel.quarkus.core.CamelRuntime;
import org.apache.camel.quarkus.core.CamelRuntimeConfig;
import org.apache.camel.quarkus.core.test.CamelTestLifecycleMainListener;
import org.apache.camel.spi.CamelContextCustomizer;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

@Recorder
public class CamelMainRecorder {
    private static final Logger LOGGER = Logger.getLogger(CamelMainRecorder.class);

    private final RuntimeValue<CamelRuntimeConfig> camelRuntimeConfig;

    public CamelMainRecorder(RuntimeValue<CamelRuntimeConfig> camelRuntimeConfig) {
        this.camelRuntimeConfig = camelRuntimeConfig;
    }

    public RuntimeValue<CamelMain> createCamelMain(RuntimeValue<CamelContext> runtime,
            BeanContainer container,
            FailureRemedy failureRemedy) {
        CamelMain main = new CamelMain(runtime.getValue(), failureRemedy);
        main.setRoutesCollector(new CamelMainRoutesCollector());

        // properties are loaded through MicroProfile Config so there's
        // no need to look for sources.
        main.setDefaultPropertyPlaceholderLocation("false");

        // register to the container
        container.beanInstance(CamelProducers.class).setMain(main);

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

    public void customizeContext(RuntimeValue<CamelMain> main, List<RuntimeValue<CamelContextCustomizer>> contextCustomizers) {
        main.getValue().addMainListener(new MainListenerSupport() {
            @Override
            public void afterConfigure(BaseMainSupport main) {
                for (RuntimeValue<CamelContextCustomizer> customizer : contextCustomizers) {
                    customizer.getValue().configure(main.getCamelContext());
                }
            }
        });
    }

    public RuntimeValue<CamelRuntime> createRuntime(
            BeanContainer beanContainer,
            RuntimeValue<CamelMain> main,
            long shutdownTimeoutMs) {
        final CamelRuntime runtime = new CamelMainRuntime(main.getValue(), shutdownTimeoutMs);

        // register to the container
        beanContainer.beanInstance(CamelProducers.class).setRuntime(runtime);

        return new RuntimeValue<>(runtime);
    }

    public void addShutdownTask(ShutdownContext shutdown, RuntimeValue<CamelRuntime> runtime) {
        shutdown.addShutdownTask(() -> {
            try {
                runtime.getValue().stop();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void start(RuntimeValue<CamelRuntime> runtime, Supplier<String[]> arguments, String camelQuarkusVersion) {
        if (camelRuntimeConfig.getValue().bootstrap().enabled()) {
            try {
                LOGGER.infof("Apache Camel Quarkus %s is starting", camelQuarkusVersion);
                runtime.getValue().start(arguments.get());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void registerCamelMainEventBridge(RuntimeValue<CamelMain> main, Set<String> observedMainEvents) {
        main.getValue().addMainListener(new CamelMainEventBridge(observedMainEvents));
    }

    public RuntimeValue<MainListener> createTestLifecycleListener(String testProfileClassName) {
        return new RuntimeValue<>(new CamelTestLifecycleMainListener(testProfileClassName));
    }

    public void customizeDevModeCamelMain(RuntimeValue<CamelMain> main) {
        Optional<String> profile = ConfigProvider.getConfig().getOptionalValue("camel.main.profile", String.class);
        main.getValue().getMainConfigurationProperties().setProfile(profile.orElse("dev"));
    }
}
