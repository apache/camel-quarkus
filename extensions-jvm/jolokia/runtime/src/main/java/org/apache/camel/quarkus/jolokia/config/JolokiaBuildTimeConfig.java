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
package org.apache.camel.quarkus.jolokia.config;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.camel.jolokia")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface JolokiaBuildTimeConfig {
    /**
     * Enables Jolokia support.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * The context path that the Jolokia agent is deployed under.
     */
    @WithDefault("jolokia")
    String path();

    /**
     * Whether to register a Quarkus management endpoint for Jolokia (default /q/jolokia).
     * When enabled this activates a management endpoint which will be accessible on a path relative to
     * ${quarkus.http.non-application-root-path}/${quarkus.camel.jolokia.server.path}.
     * If the management interface is enabled, the value will be resolved as a path relative to
     * ${quarkus.management.root-path}/${quarkus.camel.jolokia.server.path}. Note that for this feature to work you must
     * have quarkus-vertx-http on the application classpath.
     */
    @WithDefault("true")
    boolean registerManagementEndpoint();

    /**
     * Jolokia Kubernetes build time configuration.
     */
    Kubernetes kubernetes();

    interface Kubernetes {
        /**
         * When {@code true} and the quarkus-kubernetes extension is present, a container port named jolokia will
         * be added to the generated Kubernetes manifests within the container spec ports definition.
         */
        @WithDefault("true")
        boolean exposeContainerPort();
    }
}
