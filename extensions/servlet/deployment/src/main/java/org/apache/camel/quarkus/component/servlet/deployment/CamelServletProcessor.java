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
package org.apache.camel.quarkus.component.servlet.deployment;

import java.io.IOException;
import java.util.Map.Entry;

import org.apache.camel.quarkus.servlet.runtime.CamelServletConfig;
import org.apache.camel.quarkus.servlet.runtime.CamelServletConfig.ServletConfig;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.undertow.deployment.ServletBuildItem;
import io.quarkus.undertow.deployment.ServletBuildItem.Builder;

class CamelServletProcessor {

    CamelServletConfig camelServletConfig;

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.CAMEL_SERVLET);
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void build(BuildProducer<ServletBuildItem> servlet, BuildProducer<AdditionalBeanBuildItem> additionalBean)
            throws IOException {

        boolean servletCreated = false;
        if (camelServletConfig.servlet.defaultServlet.isValid()) {
            servlet.produce(
                    newServlet(ServletConfig.DEFAULT_SERVLET_NAME, camelServletConfig.servlet.defaultServlet, additionalBean));
            servletCreated = true;
        }

        for (Entry<String, ServletConfig> e : camelServletConfig.servlet.namedServlets.entrySet()) {
            if (ServletConfig.DEFAULT_SERVLET_NAME.equals(e.getKey())) {
                throw new IllegalStateException(
                        String.format("Use quarkus.camel.servlet.urlPatterns instead of quarkus.camel.servlet.%s.urlPatterns",
                                ServletConfig.DEFAULT_SERVLET_NAME));
            }
            servlet.produce(newServlet(e.getKey(), e.getValue(), additionalBean));
            servletCreated = true;
        }

        if (!servletCreated) {
            throw new IllegalStateException(
                    String.format(
                            "Map at least one servlet to a path using quarkus.camel.servlet.urlPatterns or quarkus.camel.servlet.[your-servlet-name].urlPatterns",
                            ServletConfig.DEFAULT_SERVLET_NAME));
        }

    }

    static ServletBuildItem newServlet(final String key, final ServletConfig servletConfig,
            final BuildProducer<AdditionalBeanBuildItem> additionalBean) {
        final String servletName = servletConfig.getEffectiveServletName(key);
        if (servletConfig.urlPatterns.isEmpty()) {
            throw new IllegalStateException(String.format("Missing quarkus.camel.servlet%s.url-patterns",
                    ServletConfig.DEFAULT_SERVLET_NAME.equals(servletName) ? "" : "." + servletName));
        }
        final Builder builder = ServletBuildItem.builder(servletName, servletConfig.servletClass);
        additionalBean.produce(new AdditionalBeanBuildItem(servletConfig.servletClass));
        for (String pattern : servletConfig.urlPatterns) {
            builder.addMapping(pattern);
        }
        return builder.build();
    }

}
