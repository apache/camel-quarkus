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

    /**
     * The default servlet with implicit name `ServletConfig.DEFAULT_SERVLET_NAME`
     *
     * @asciidoclet
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public ServletConfig defaultServlet;

    /**
     * A collection of named servlets
     *
     * @asciidoclet
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public Map<String, ServletConfig> namedServlets;

    /**
     * {@code camel-servlet} component configuration
     */
    @ConfigGroup
    public static class ServletConfig {

        public static final String DEFAULT_SERVLET_NAME = "CamelServlet";

        public static final String DEFAULT_SERVLET_CLASS = "org.apache.camel.component.servlet.CamelHttpTransportServlet";

        /**
         * A comma separated list of path patterns under which the CamelServlet should be accessible. Example path patterns:
         * `/++*++`, `/services/++*++`
         *
         * @asciidoclet
         */
        @ConfigItem
        public Optional<List<String>> urlPatterns;

        /**
         * A fully qualified name of a servlet class to serve paths that match `url-patterns`
         *
         * @asciidoclet
         */
        @ConfigItem(defaultValue = DEFAULT_SERVLET_CLASS)
        public String servletClass;

        /**
         * A servletName as it would be defined in a `web.xml` file or in the `jakarta.servlet.annotation.WebServlet++#++name()`
         * annotation.
         *
         * @asciidoclet
         */
        @ConfigItem(defaultValue = DEFAULT_SERVLET_NAME)
        public String servletName;

        /**
         * Sets the loadOnStartup priority on the Servlet. A loadOnStartup is a value greater than or equal to zero, indicates
         * to the container the initialization priority of the Servlet. If loadOnStartup is a negative integer, the Servlet is
         * initialized lazily.
         *
         * @asciidoclet
         */
        @ConfigItem(defaultValue = "-1")
        public Integer loadOnStartup;

        /**
         * Enables Camel to benefit from asynchronous Servlet support.
         *
         * @asciidoclet
         */
        @ConfigItem(defaultValue = "false")
        public boolean async;

        /**
         * When set to `true` used in conjunction with `quarkus.camel.servlet.async = true`, this will force route processing to
         * run synchronously.
         *
         * @asciidoclet
         */
        @ConfigItem(defaultValue = "false")
        public boolean forceAwait;

        /**
         * The name of a bean to configure an optional custom thread pool for handling Camel Servlet processing.
         *
         * @asciidoclet
         */
        @ConfigItem
        public Optional<String> executorRef;

        /**
         * Servlet multipart request configuration.
         *
         * @asciidoclet
         */
        public MultipartConfig multipart;

        /**
         * @return      {@code true} if this {@link ServletConfig} is valid as a whole. This currently translates to
         *              {@link #urlPatterns} being non-empty because {@link #servletClass} and {@link #servletName} have
         *              default values. Otherwise returns {@code false}.
         * @asciidoclet
         */
        public boolean isValid() {
            return urlPatterns.isPresent() && !urlPatterns.get().isEmpty();
        }

        /**
         * Setting the servlet name is possible both via `servlet-name` and the key in the
         * `org.apache.camel.quarkus.servlet.runtime.CamelServletConfig.ServletsConfig++#++namedServlets` map. This method sets
         * the precedence: the `servlet-name` gets effective only if it has a non-default value; otherwise the `key` is returned
         * as the servlet name.
         *
         * @param       key the key used in
         *                  {@link org.apache.camel.quarkus.servlet.runtime.CamelServletConfig.ServletsConfig#namedServlets}
         * @return          the effective servlet name to use
         * @asciidoclet
         */
        public String getEffectiveServletName(final String key) {
            return DEFAULT_SERVLET_NAME.equals(servletName) ? key : servletName;
        }

        /**
         * Servlet multipart request configuration.
         */
        @ConfigGroup
        public static class MultipartConfig {

            /**
             * An absolute path to a directory on the file system to store files temporarily while the parts are processed or
             * when the size of the file exceeds the specified file-size-threshold configuration value.
             *
             * @asciidoclet
             */
            @ConfigItem(defaultValue = "${java.io.tmpdir}")
            public String location;

            /**
             * The maximum size allowed in bytes for uploaded files. The default size (-1) allows an unlimited size.
             *
             * @asciidoclet
             */
            @ConfigItem(defaultValue = "-1")
            public long maxFileSize;

            /**
             * The maximum size allowed in bytes for a multipart/form-data request. The default size (-1) allows an unlimited
             * size.
             *
             * @asciidoclet
             */
            @ConfigItem(defaultValue = "-1")
            public long maxRequestSize;

            /**
             * The file size in bytes after which the file will be temporarily stored on disk.
             *
             * @asciidoclet
             */
            @ConfigItem(defaultValue = "0")
            public int fileSizeThreshold;
        }
    }
}
