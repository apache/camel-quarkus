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

import java.io.File;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.camel.jolokia")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface JolokiaRuntimeConfig {
    /**
     * Jolokia agent HTTP server configuration.
     */
    Server server();

    /**
     * Kubernetes runtime configuration.
     */
    Kubernetes kubernetes();

    /**
     * Arbitrary Jolokia configuration options. These are described at the
     * https://jolokia.org/reference/html/manual/agents.html[Jolokia documentation].
     * Options can be configured like `quarkus.camel.jolokia.additional-properties."debug"=true`.
     */
    Map<String, String> additionalProperties();

    /**
     * When `true`, a Jolokia restrictor is registered that limits MBean read, write and operation execution to the
     * following MBean domains.
     *
     * * org.apache.camel
     * * java.lang
     * * java.nio
     *
     * Note that this option has no effect if `quarkus.camel.jolokia.additional-properties."restrictorClass"` is set.
     */
    @WithDefault("true")
    boolean registerCamelRestrictor();

    interface Server {
        /**
         * Whether the Jolokia agent HTTP server should be started automatically.
         * When set to `false`, it is the user responsibility to start the server.
         * This can be done via `@Inject CamelQuarkusJolokiaServer` and then invoking the `start()` method.
         */
        @WithDefault("true")
        boolean autoStart();

        /**
         * The host address to which the Jolokia agent HTTP server should bind to.
         * When unspecified, the default is localhost for dev and test mode.
         * In prod mode the default is to bind to all interfaces at 0.0.0.0.
         */
        Optional<String> host();

        /**
         * The port on which the Jolokia agent HTTP server should listen on.
         */
        @WithDefault("8778")
        int port();

        /**
         * The mode in which Jolokia agent discovery is enabled. The default `dev-test`, enables discovery only in dev and
         * test modes.
         * A value of `all` enables agent discovery in dev, test and prod modes. Setting the value to `none` will
         * disable agent discovery in all modes.
         */
        @WithDefault("DEV_TEST")
        DiscoveryEnabledMode discoveryEnabledMode();
    }

    enum DiscoveryEnabledMode {
        ALL,
        DEV_TEST,
        NONE,
    }

    interface Kubernetes {
        /**
         * Whether to enable Jolokia SSL client authentication in Kubernetes environments.
         * Useful for tools such as hawtio to be able to connect with your application.
         */
        @WithDefault("true")
        boolean clientAuthenticationEnabled();

        /**
         * Absolute path of the CA certificate Jolokia should use for SSL client authentication.
         */
        @WithDefault("/var/run/secrets/kubernetes.io/serviceaccount/service-ca.crt")
        File serviceCaCert();

        /**
         * The principal which must be given in a client certificate to allow access to Jolokia.
         */
        Optional<String> clientPrincipal();
    }
}
