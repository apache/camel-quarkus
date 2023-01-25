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
package org.apache.camel.quarkus.servlet.runtime;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * {@link ConfigRoot} for {@link #defaultServlet}.
 */
@ConfigRoot(name = "camel.servlet", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public final class CamelServletConfig {

    /** The default servlet with implicit name {@value ServletConfig.DEFAULT_SERVLET_NAME} */
    @ConfigItem(name = ConfigItem.PARENT)
    public ServletConfig defaultServlet;

    /** A collection of named servlets */
    @ConfigItem(name = ConfigItem.PARENT)
    public Map<String, ServletConfig> namedServlets;

    /** {@code camel-servlet} component configuration */
    @ConfigGroup
    public static class ServletConfig {

        public static final String DEFAULT_SERVLET_NAME = "CamelServlet";
        public static final String DEFAULT_SERVLET_CLASS = "org.apache.camel.component.servlet.CamelHttpTransportServlet";

        /**
         * A comma separated list of path patterns under which the CamelServlet should be accessible. Example path
         * patterns: {@code /*}, {@code /services/*}
         */
        @ConfigItem
        public Optional<List<String>> urlPatterns;

        /** A fully qualified name of a servlet class to serve paths that match {@link #urlPatterns} */
        @ConfigItem(defaultValue = DEFAULT_SERVLET_CLASS)
        public String servletClass;

        /**
         * A servletName as it would be defined in a `web.xml` file or in the
         * {@link jakarta.servlet.annotation.WebServlet#name()} annotation.
         */
        @ConfigItem(defaultValue = DEFAULT_SERVLET_NAME)
        public String servletName;

        /**
         * @return {@code true} if this {@link ServletConfig} is valid as a whole. This currently translates to
         *         {@link #urlPatterns} being non-empty because {@link #servletClass} and {@link #servletName} have
         *         default values. Otherwise returns {@code false}.
         */
        public boolean isValid() {
            return urlPatterns.isPresent() && !urlPatterns.get().isEmpty();
        }

        /**
         * Setting the servlet name is possible both via {@link #servletName} and the key in the
         * {@link org.apache.camel.quarkus.servlet.runtime.CamelServletConfig.ServletsConfig#namedServlets} map. This method
         * sets the precedence: the {@link #servletName} gets effective only if it has a non-default value; otherwise
         * the {@code key} is returned as the servlet name.
         *
         * @param  key the key used in
         *             {@link org.apache.camel.quarkus.servlet.runtime.CamelServletConfig.ServletsConfig#namedServlets}
         * @return     the effective servlet name to use
         */
        public String getEffectiveServletName(final String key) {
            return DEFAULT_SERVLET_NAME.equals(servletName) ? key : servletName;
        }

    }

}
