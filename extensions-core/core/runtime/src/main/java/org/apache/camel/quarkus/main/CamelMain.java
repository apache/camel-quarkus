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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.quarkus.runtime.Quarkus;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.main.MainCommandLineSupport;
import org.apache.camel.main.MainConfigurationProperties;
import org.apache.camel.main.MainListener;
import org.apache.camel.main.MainShutdownStrategy;
import org.apache.camel.main.RoutesConfigurer;
import org.apache.camel.main.SimpleMainShutdownStrategy;
import org.apache.camel.quarkus.core.CamelConfig.FailureRemedy;
import org.apache.camel.spi.HasCamelContext;
import org.apache.camel.support.service.ServiceHelper;
import org.apache.camel.util.StringHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CamelMain extends MainCommandLineSupport implements HasCamelContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(CamelMain.class);

    private final AtomicBoolean engineStarted;
    private final FailureRemedy failureRemedy;

    public CamelMain(CamelContext camelContext, FailureRemedy failureRemedy) {
        this.camelContext = camelContext;
        this.engineStarted = new AtomicBoolean();
        this.failureRemedy = failureRemedy;
    }

    protected void configureRoutes(CamelContext camelContext) throws Exception {
        // then configure and add the routes
        RoutesConfigurer configurer = new RoutesConfigurer();

        if (mainConfigurationProperties.isRoutesCollectorEnabled()) {
            configurer.setRoutesCollector(routesCollector);
        }

        configurer.setBeanPostProcessor(camelContext.getCamelContextExtension().getBeanPostProcessor());
        configurer.setRoutesBuilders(mainConfigurationProperties.getRoutesBuilders());
        configurer.setRoutesExcludePattern(mainConfigurationProperties.getRoutesExcludePattern());
        configurer.setRoutesIncludePattern(mainConfigurationProperties.getRoutesIncludePattern());
        configurer.setJavaRoutesExcludePattern(mainConfigurationProperties.getJavaRoutesExcludePattern());
        configurer.setJavaRoutesIncludePattern(mainConfigurationProperties.getJavaRoutesIncludePattern());
        configurer.configureRoutes(camelContext);
    }

    @Override
    protected void configureStartupRecorder(CamelContext camelContext) {
        super.configureStartupRecorder(camelContext);
    }

    @Override
    protected void doInit() throws Exception {
        MainShutdownStrategy shutdownStrategy = new SimpleMainShutdownStrategy();
        shutdownStrategy.addShutdownListener(() -> Quarkus.asyncExit(getExitCode()));

        setShutdownStrategy(shutdownStrategy);

        super.doInit();
        initCamelContext();
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        try {
            // if we were veto started then mark as completed
            this.camelContext.start();
        } finally {
            if (getCamelContext().isVetoStarted()) {
                completed();
            }
        }
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        this.camelContext.stop();
        this.engineStarted.set(false);
    }

    @Override
    protected ProducerTemplate findOrCreateCamelTemplate() {
        return this.camelContext.createProducerTemplate();
    }

    @Override
    protected void initCamelContext() throws Exception {
        postProcessCamelContext(camelContext);
    }

    @Override
    protected CamelContext createCamelContext() {
        throw new IllegalStateException("Should not be invoked");
    }

    public Collection<MainListener> getMainListeners() {
        return Collections.unmodifiableCollection(listeners);
    }

    public MainConfigurationProperties getMainConfigurationProperties() {
        return mainConfigurationProperties;
    }

    /**
     * Start the engine.
     */
    public void startEngine() throws Exception {
        if (shutdownStrategy.isRunAllowed() && engineStarted.compareAndSet(false, true)) {
            init();
            internalBeforeStart();
            beforeStart();
            start();
            afterStart();
        }
    }

    /**
     * Start the engine if not done already and wait until completed, or the JVM terminates.
     */
    public void runEngine() throws Exception {
        if (shutdownStrategy.isRunAllowed()) {
            startEngine();
            waitUntilCompleted();

            try {
                if (camelTemplate != null) {
                    ServiceHelper.stopService(camelTemplate);
                    camelTemplate = null;
                }
            } catch (Exception e) {
                LOG.debug("Error stopping camelTemplate due " + e.getMessage() + ". This exception is ignored.", e);
            }

            beforeStop();
            stop();
            afterStop();
        }
    }

    @Override
    public int run(String[] args) throws Exception {
        parseArguments(args);
        runEngine();
        return getExitCode();
    }

    @Override
    public void parseArguments(String[] arguments) {
        LinkedList<String> args = new LinkedList<>(Arrays.asList(arguments));
        List<String> unknownArgs = new ArrayList<>();

        boolean valid = true;
        while (!args.isEmpty()) {
            initOptions();
            String arg = args.removeFirst();
            boolean handled = false;
            for (Option option : options) {
                if (option.processOption(arg, args)) {
                    handled = true;
                    break;
                }
            }
            if (!handled && !failureRemedy.equals(FailureRemedy.ignore)) {
                if (arg.length() >= 100) {
                    // For long arguments, clean up formatting for console output
                    String truncatedArg = String.format("%s...", StringHelper.limitLength(arg, 97));
                    unknownArgs.add(truncatedArg);
                } else {
                    unknownArgs.add(arg);
                }
                valid = false;
            }
        }
        if (!valid) {
            System.out.println("Unknown option: " + String.join(" ", unknownArgs));
            System.out.println();
            showOptions();
            if (failureRemedy.equals(FailureRemedy.fail)) {
                completed();
                throw new RuntimeException("CamelMain encountered unknown arguments");
            }
        }
    }
}
