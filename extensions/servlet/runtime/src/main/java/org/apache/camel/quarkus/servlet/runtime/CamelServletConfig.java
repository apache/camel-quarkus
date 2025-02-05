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

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithParentName;

/**
 * {@link ConfigRoot} for {@link #defaultServlet}.
 */
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
@ConfigMapping(prefix = "quarkus.camel.servlet")
public interface CamelServletConfig {

    /**
     * The default servlet with implicit name `ServletConfig.DEFAULT_SERVLET_NAME`
     *
     * @asciidoclet
     */
    @WithParentName
    ServletConfig defaultServlet();

    /**
     * A collection of named servlets
     *
     * @asciidoclet
     */
    @WithParentName
    Map<String, ServletConfig> namedServlets();

    /**
     * {@code camel-servlet} component configuration
     */
    interface ServletConfig {
        String DEFAULT_SERVLET_NAME = "CamelServlet";
        String DEFAULT_SERVLET_CLASS = "org.apache.camel.component.servlet.CamelHttpTransportServlet";

        /**
         * A comma separated list of path patterns under which the CamelServlet should be accessible. Example path patterns:
         * `/++*++`, `/services/++*++`
         *
         * @asciidoclet
         */
        Optional<List<String>> urlPatterns();

        /**
         * A fully qualified name of a servlet class to serve paths that match `url-patterns`
         *
         * @asciidoclet
         */
        @WithDefault(DEFAULT_SERVLET_CLASS)
        String servletClass();

        /**
         * A servletName as it would be defined in a `web.xml` file or in the `jakarta.servlet.annotation.WebServlet++#++name()`
         * annotation.
         *
         * @asciidoclet
         */
        @WithDefault(DEFAULT_SERVLET_NAME)
        String servletName();

        /**
         * Sets the loadOnStartup priority on the Servlet. A loadOnStartup is a value greater than or equal to zero, indicates
         * to the container the initialization priority of the Servlet. If loadOnStartup is a negative integer, the Servlet is
         * initialized lazily.
         *
         * @asciidoclet
         */
        @WithDefault("-1")
        Integer loadOnStartup();

        /**
         * Enables Camel to benefit from asynchronous Servlet support.
         *
         * @asciidoclet
         */
        @WithDefault("false")
        boolean async();

        /**
         * When set to `true` used in conjunction with `quarkus.camel.servlet.async = true`, this will force route processing to
         * run synchronously.
         *
         * @asciidoclet
         */
        @WithDefault("false")
        boolean forceAwait();

        /**
         * The name of a bean to configure an optional custom thread pool for handling Camel Servlet processing.
         *
         * @asciidoclet
         */
        Optional<String> executorRef();

        /**
         * Servlet multipart request configuration.
         *
         * @asciidoclet
         */
        MultipartConfig multipart();

        /**
         * @return      {@code true} if this {@link ServletConfig} is valid as a whole. This currently translates to
         *              {@link #urlPatterns} being non-empty because {@link #servletClass} and {@link #servletName} have
         *              default values. Otherwise returns {@code false}.
         * @asciidoclet
         */
        default boolean isValid() {
            return urlPatterns().isPresent() && !urlPatterns().get().isEmpty();
        }

        /**
         * Setting the servlet name is possible both via `servlet-name` and the key in the
         * `org.apache.camel.quarkus.servlet.runtime.CamelServletConfig.ServletsConfig++#++namedServlets` map. This method sets
         * the precedence: the `servlet-name` gets effective only if it has a non-default value; otherwise the `key` is returned
         * as the servlet name.
         *
         * @param       key the key used in
         *                  {@link org.apache.camel.quarkus.servlet.runtime.CamelServletConfig#namedServlets}
         * @return          the effective servlet name to use
         * @asciidoclet
         */
        default String getEffectiveServletName(final String key) {
            return DEFAULT_SERVLET_NAME.equals(servletName()) ? key : servletName();
        }

        /**
         * Servlet multipart request configuration.
         */
        interface MultipartConfig {
            /**
             * An absolute path to a directory on the file system to store files temporarily while the parts are processed or
             * when the size of the file exceeds the specified file-size-threshold configuration value.
             *
             * @asciidoclet
             */
            @WithDefault("${java.io.tmpdir}")
            String location();

            /**
             * The maximum size allowed in bytes for uploaded files. The default size (-1) allows an unlimited size.
             *
             * @asciidoclet
             */
            @WithDefault("-1")
            long maxFileSize();

            /**
             * The maximum size allowed in bytes for a multipart/form-data request. The default size (-1) allows an unlimited
             * size.
             *
             * @asciidoclet
             */
            @WithDefault("-1")
            long maxRequestSize();

            /**
             * The file size in bytes after which the file will be temporarily stored on disk.
             *
             * @asciidoclet
             */
            @WithDefault("0")
            int fileSizeThreshold();
        }
    }
}
