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

import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.undertow.deployment.ServletBuildItem;
import io.quarkus.undertow.deployment.ServletBuildItem.Builder;
import jakarta.servlet.MultipartConfigElement;
import org.apache.camel.quarkus.servlet.runtime.CamelServletConfig;
import org.apache.camel.quarkus.servlet.runtime.CamelServletConfig.ServletConfig;
import org.apache.camel.quarkus.servlet.runtime.CamelServletConfig.ServletConfig.MultipartConfig;

class ServletProcessor {
    private static final String FEATURE = "camel-servlet";

    CamelServletConfig camelServletConfig;

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void build(BuildProducer<ServletBuildItem> servlet) {
        boolean servletCreated = false;
        if (camelServletConfig.defaultServlet.isValid()) {
            servlet.produce(
                    newServlet(ServletConfig.DEFAULT_SERVLET_NAME, camelServletConfig.defaultServlet));
            servletCreated = true;
        }

        for (Entry<String, ServletConfig> e : camelServletConfig.namedServlets.entrySet()) {
            if (ServletConfig.DEFAULT_SERVLET_NAME.equals(e.getKey())) {
                throw new IllegalStateException(
                        String.format("Use quarkus.camel.servlet.url-patterns instead of quarkus.camel.servlet.%s.url-patterns",
                                ServletConfig.DEFAULT_SERVLET_NAME));
            }
            servlet.produce(newServlet(e.getKey(), e.getValue()));
            servletCreated = true;
        }

        if (!servletCreated) {
            throw new IllegalStateException(
                    "Map at least one servlet to a path using quarkus.camel.servlet.url-patterns or quarkus.camel.servlet.[your-servlet-name].url-patterns");
        }

    }

    static ServletBuildItem newServlet(String key, ServletConfig servletConfig) {
        final String servletName = servletConfig.getEffectiveServletName(key);
        final Optional<List<String>> urlPatterns = servletConfig.urlPatterns;
        if (!urlPatterns.isPresent() || urlPatterns.get().isEmpty()) {
            throw new IllegalStateException(
                    String.format("Missing quarkus.camel.servlet%s.url-patterns",
                            ServletConfig.DEFAULT_SERVLET_NAME.equals(servletName) ? "" : "." + servletName));
        }

        final Builder builder = ServletBuildItem.builder(servletName, servletConfig.servletClass);
        for (String pattern : urlPatterns.get()) {
            builder.addMapping(pattern);
        }

        MultipartConfig multipartConfig = servletConfig.multipart;
        if (multipartConfig != null) {
            builder.setMultipartConfig(new MultipartConfigElement(
                    multipartConfig.location,
                    multipartConfig.maxFileSize,
                    multipartConfig.maxRequestSize,
                    multipartConfig.fileSizeThreshold));
        }

        return builder.build();
    }
}
