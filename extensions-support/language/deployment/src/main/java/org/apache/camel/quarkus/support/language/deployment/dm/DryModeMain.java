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
package org.apache.camel.quarkus.support.language.deployment.dm;

import java.util.Collection;

import org.apache.camel.CamelContext;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.main.MainSupport;

/**
 * {@code DryModeMain} a specific main implementation allowing to do a dry run of the application in order to collect
 * the expressions defined in all the routes of the project.
 */
public class DryModeMain extends MainSupport {

    private final DryModeLanguageResolver languageResolver = new DryModeLanguageResolver();

    public DryModeMain(String appName, Class<?>[] routeBuilderClasses) {
        setAppName(appName);
        mainConfigurationProperties.addRoutesBuilder(routeBuilderClasses);
    }

    @Override
    protected ProducerTemplate findOrCreateCamelTemplate() {
        final CamelContext context = getCamelContext();
        if (context == null) {
            return null;
        }
        return context.createProducerTemplate();
    }

    @Override
    protected CamelContext createCamelContext() {
        DefaultCamelContext ctx = new DefaultCamelContext(false);
        ctx.setName(getAppName());
        ctx.setInjector(new DryModeInjector(ctx.getInjector()));

        ExtendedCamelContext extendedCamelContext = ctx.getCamelContextExtension();
        extendedCamelContext.addContextPlugin(DryModeLanguageResolver.class, languageResolver);
        extendedCamelContext.addContextPlugin(DryModeComponentResolver.class, new DryModeComponentResolver());
        return ctx;
    }

    @Override
    protected void doInit() throws Exception {
        // turn off auto-wiring when running in dry mode
        mainConfigurationProperties.setAutowiredEnabled(false);
        // and turn off fail fast as we stub components
        mainConfigurationProperties.setAutoConfigurationFailFast(false);
        mainConfigurationProperties.setAutoStartup(false);
        mainConfigurationProperties.setDurationMaxSeconds(1);
        mainConfigurationProperties.setAutoConfigurationLogSummary(false);
        super.doInit();
        initCamelContext();
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        CamelContext context = getCamelContext();
        if (context != null) {
            context.start();
        }
    }

    public Collection<DryModeLanguage> getLanguages() {
        return languageResolver.getLanguages();
    }
}
