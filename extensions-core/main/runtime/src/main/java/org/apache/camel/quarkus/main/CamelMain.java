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

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.quarkus.runtime.Quarkus;
import org.apache.camel.CamelContext;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.main.MainCommandLineSupport;
import org.apache.camel.main.MainConfigurationProperties;
import org.apache.camel.main.MainListener;
import org.apache.camel.main.MainShutdownStrategy;
import org.apache.camel.spi.CamelBeanPostProcessor;
import org.apache.camel.spi.HasCamelContext;
import org.apache.camel.support.service.ServiceHelper;

public final class CamelMain extends MainCommandLineSupport implements HasCamelContext {
    public CamelMain(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    @Override
    protected void loadRouteBuilders(CamelContext camelContext) throws Exception {
        // routes are discovered and pre-instantiated which allow to post process them to support Camel's DI
        CamelBeanPostProcessor postProcessor = camelContext.adapt(ExtendedCamelContext.class).getBeanPostProcessor();
        for (RoutesBuilder builder : mainConfigurationProperties.getRoutesBuilders()) {
            postProcessor.postProcessBeforeInitialization(builder, builder.getClass().getName());
            postProcessor.postProcessAfterInitialization(builder, builder.getClass().getName());
        }
    }

    @Override
    protected void doInit() throws Exception {
        setShutdownStrategy(new ShutdownStrategy());

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

    Collection<MainListener> getMainListeners() {
        return Collections.unmodifiableCollection(listeners);
    }

    MainConfigurationProperties getMainConfigurationProperties() {
        return mainConfigurationProperties;
    }

    public void initAndStart() throws Exception {
        if (shutdownStrategy.isRunAllowed()) {
            init();
            // if we have an issue starting then propagate the exception to caller
            beforeStart();
            start();
            try {
                afterStart();
            } catch (Exception e) {
                // however while running then just log errors
                LOG.error("Failed: {}", e, e);
            }
        }
    }

    public void run() throws Exception {
        if (shutdownStrategy.isRunAllowed()) {
            try {
                waitUntilCompleted();
                internalBeforeStop();
                beforeStop();
                stop();
                afterStop();
            } catch (Exception e) {
                // however while running then just log errors
                LOG.error("Failed: {}", e, e);
            }
        }
    }

    private void internalBeforeStop() {
        try {
            if (camelTemplate != null) {
                ServiceHelper.stopService(camelTemplate);
                camelTemplate = null;
            }
        } catch (Exception e) {
            LOG.debug("Error stopping camelTemplate due " + e.getMessage() + ". This exception is ignored.", e);
        }
    }

    /**
     * Implementation of a {@link MainShutdownStrategy} based on Quarkus Command Mode.
     *
     * @see <a href="https://quarkus.io/guides/command-mode-reference">Quarkus Command Mode Applications</a>
     */
    private class ShutdownStrategy implements MainShutdownStrategy {
        private final AtomicBoolean completed;
        private final CountDownLatch latch;

        public ShutdownStrategy() {
            this.completed = new AtomicBoolean(false);
            this.latch = new CountDownLatch(1);
        }

        @Override
        public boolean isRunAllowed() {
            return !completed.get();
        }

        @Override
        public boolean shutdown() {
            if (completed.compareAndSet(false, true)) {
                latch.countDown();
                Quarkus.asyncExit(getExitCode());
                return true;
            }

            return false;
        }

        @Override
        public void await() throws InterruptedException {
            latch.await();
        }

        @Override
        public void await(long timeout, TimeUnit unit) throws InterruptedException {
            latch.await(timeout, unit);
        }
    }
}
