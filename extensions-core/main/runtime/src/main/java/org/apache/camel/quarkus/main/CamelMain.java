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

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.main.BaseMainSupport;
import org.apache.camel.main.MainConfigurationProperties;
import org.apache.camel.main.MainListener;
import org.apache.camel.spi.CamelBeanPostProcessor;
import org.apache.camel.support.service.ServiceHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CamelMain extends BaseMainSupport implements CamelContextAware {
    private static final Logger LOGGER = LoggerFactory.getLogger(CamelMain.class);

    @Override
    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    @Override
    protected void doInit() throws Exception {
        super.doInit();
        postProcessCamelContext(getCamelContext());
        getCamelContext().init();
    }

    @Override
    protected void doStart() throws Exception {
        for (MainListener listener : listeners) {
            listener.beforeStart(this);
        }

        getCamelContext().start();

        for (MainListener listener : listeners) {
            listener.afterStart(this);
        }
    }

    @Override
    protected void postProcessCamelContext(CamelContext camelContext) throws Exception {
        super.postProcessCamelContext(camelContext);

        // post process classes with camel's post processor so classes have support
        // for camel's simple di
        CamelBeanPostProcessor postProcessor = camelContext.adapt(ExtendedCamelContext.class).getBeanPostProcessor();
        for (RoutesBuilder builder : mainConfigurationProperties.getRoutesBuilders()) {
            postProcessor.postProcessBeforeInitialization(builder, builder.getClass().getName());
            postProcessor.postProcessAfterInitialization(builder, builder.getClass().getName());
        }
    }

    @Override
    protected void loadRouteBuilders(CamelContext camelContext) throws Exception {
        // classes are automatically discovered by build processors
    }

    @Override
    protected void doStop() throws Exception {
        try {
            if (camelTemplate != null) {
                ServiceHelper.stopService(camelTemplate);
                camelTemplate = null;
            }
        } catch (Exception e) {
            LOGGER.debug("Error stopping camelTemplate due " + e.getMessage() + ". This exception is ignored.", e);
        }

        for (MainListener listener : listeners) {
            listener.beforeStop(this);
        }

        getCamelContext().stop();

        for (MainListener listener : listeners) {
            listener.afterStop(this);
        }
    }

    @Override
    protected ProducerTemplate findOrCreateCamelTemplate() {
        return getCamelContext().createProducerTemplate();
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
}
