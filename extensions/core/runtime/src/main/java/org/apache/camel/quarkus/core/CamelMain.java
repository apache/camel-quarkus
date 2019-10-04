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

import java.util.Collection;
import java.util.Collections;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.main.MainConfigurationProperties;
import org.apache.camel.main.MainListener;
import org.apache.camel.main.MainSupport;
import org.apache.camel.support.service.ServiceHelper;

public class CamelMain extends MainSupport implements CamelContextAware {
    @Override
    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    @Override
    protected void doInit() throws Exception {
        postProcessCamelContext(getCamelContext());
    }

    @Override
    protected void doStart() throws Exception {
        beforeStart();
        getCamelContext().start();
        afterStart();
    }

    @Override
    protected void doStop() throws Exception {
        try {
            if (camelTemplate != null) {
                ServiceHelper.stopService(camelTemplate);
                camelTemplate = null;
            }
        } catch (Exception e) {
            MainSupport.LOG.debug("Error stopping camelTemplate due " + e.getMessage() + ". This exception is ignored.", e);
        }

        beforeStop();
        getCamelContext().stop();
        afterStop();
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
