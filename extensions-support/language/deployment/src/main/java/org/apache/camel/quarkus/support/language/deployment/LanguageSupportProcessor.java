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
package org.apache.camel.quarkus.support.language.deployment;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.quarkus.core.CamelConfig;
import org.apache.camel.quarkus.core.deployment.spi.CamelRoutesBuilderClassBuildItem;
import org.apache.camel.quarkus.support.language.deployment.dm.DryModeLanguage;
import org.apache.camel.quarkus.support.language.deployment.dm.DryModeMain;
import org.apache.camel.quarkus.support.language.deployment.dm.ExpressionHolder;
import org.apache.camel.quarkus.support.language.deployment.dm.ScriptHolder;
import org.jboss.logging.Logger;

class LanguageSupportProcessor {

    private static final Logger LOG = Logger.getLogger(LanguageSupportProcessor.class);

    @BuildStep
    ExpressionExtractionResultBuildItem extractExpressions(CamelConfig config,
            List<CamelRoutesBuilderClassBuildItem> routesBuilderClasses,
            BuildProducer<ExpressionBuildItem> expressions,
            BuildProducer<ScriptBuildItem> scripts) throws Exception {
        if (config.expression.extractionEnabled) {
            final ClassLoader loader = Thread.currentThread().getContextClassLoader();
            if (!(loader instanceof QuarkusClassLoader)) {
                throw new IllegalStateException(
                        QuarkusClassLoader.class.getSimpleName() + " expected as the context class loader");
            }
            final List<Class<?>> routeBuilderClasses = new ArrayList<>(routesBuilderClasses.size());
            for (CamelRoutesBuilderClassBuildItem routesBuilderClass : routesBuilderClasses) {
                final String className = routesBuilderClass.getDotName().toString();
                final Class<?> cl = loader.loadClass(className);

                if (RouteBuilder.class.isAssignableFrom(cl)) {
                    routeBuilderClasses.add(cl);
                } else {
                    LOG.warnf("Language expressions occurring in %s won't be compiled at build time", cl);
                }
            }
            try {
                DryModeMain main = new DryModeMain("Expression Extractor", routeBuilderClasses.toArray(new Class<?>[0]));
                main.start();
                main.run();
                for (DryModeLanguage language : main.getLanguages()) {
                    final String name = language.getName();
                    for (ExpressionHolder holder : language.getPredicates()) {
                        expressions.produce(new ExpressionBuildItem(name, holder.getContent(), holder.getProperties(), true));
                    }
                    for (ExpressionHolder holder : language.getExpressions()) {
                        expressions.produce(new ExpressionBuildItem(name, holder.getContent(), holder.getProperties(), false));
                    }
                    for (ScriptHolder script : language.getScripts()) {
                        scripts.produce(new ScriptBuildItem(name, script.getContent(), script.getBindings()));
                    }
                }
                return new ExpressionExtractionResultBuildItem(true);
            } catch (Exception e) {
                switch (config.expression.onBuildTimeAnalysisFailure) {
                case fail:
                    throw new RuntimeException(
                            "Could not extract language expressions."
                                    + "You may want to set quarkus.camel.expression.on-build-time-analysis-failure to warn or ignore if you do not use languages in your routes",
                            e);
                case warn:
                    LOG.warn("Could not extract language expressions.", e);
                    break;
                case ignore:
                    LOG.debug("Could not extract language expressions", e);
                    break;
                default:
                    throw new IllegalStateException("Unexpected " + CamelConfig.FailureRemedy.class.getSimpleName() + ": "
                            + config.expression.onBuildTimeAnalysisFailure);
                }
            }
        }
        return new ExpressionExtractionResultBuildItem(false);
    }
}
