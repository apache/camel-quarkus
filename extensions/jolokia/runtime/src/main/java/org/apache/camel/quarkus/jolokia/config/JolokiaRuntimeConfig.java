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
import java.util.List;
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

    /**
     * When `true`, the default Camel Jolokia restrictor allows connections from non-loopback (remote) addresses.
     * When `false` (the default), only connections from loopback addresses (e.g. 127.0.0.1, ::1) are permitted.
     *
     * This controls client addresses only. Cross-origin requests remain restricted to loopback origins and
     * whatever `quarkus.camel.jolokia.allowed-origins` lists.
     *
     * This option only takes effect when `register-camel-restrictor` is `true` and a custom restrictor class
     * is not configured via `quarkus.camel.jolokia.additional-properties."restrictorClass"`. It is ignored when a
     * Jolokia access policy (`jolokia-access.xml`) carries a `<remote>` section, since that section decides
     * client addresses outright. A policy with no `<remote>` section says nothing about addresses, so this
     * option still applies.
     *
     * It is not needed on Kubernetes when
     * `quarkus.camel.jolokia.kubernetes.client-authentication-enabled` is `true` and the service CA certificate
     * is present, since SSL client authentication then authenticates every client and non-loopback addresses are
     * already accepted.
     */
    @WithDefault("false")
    boolean remoteAccessAllowed();

    /**
     * Origins from which the default Camel Jolokia restrictor accepts cross-origin requests, in addition to
     * loopback origins and requests that carry no `Origin` header. That loopback exception still applies once
     * `quarkus.camel.jolokia.remote-access-allowed` has opened the agent to clients on other hosts.
     *
     * Each value is matched against the scheme, host and port of the request `Origin` header, for example
     * `https://myhost.example.com`. A port that is the default for the scheme is optional. Matching is
     * case-insensitive and `*` is a wildcard, as in the `<allow-origin>` rules of a Jolokia access policy, so
     * `*://*.example.com` covers a whole domain and `*` on its own accepts any origin.
     *
     * Jolokia falls back to the `Referer` header when a request carries no `Origin`. Such a value is reduced to
     * its origin before being matched, so only the origin needs listing.
     *
     * This is the only way to allow an origin. The `<cors>` section of a Jolokia access policy
     * (`jolokia-access.xml`) is not used, though `<allow-origin>` values can be copied here unchanged and
     * `<ignore-scheme/>` has an equivalent in `quarkus.camel.jolokia.ignore-origin-scheme`. The rest of a
     * policy is applied as normal.
     *
     * Note that Jolokia itself rejects a request whose `Origin` uses `https` when the agent is serving plain
     * `http`, whatever this option is set to.
     *
     * On Kubernetes, cross-origin requests are accepted without this option once
     * `quarkus.camel.jolokia.kubernetes.client-principal` pins which client identity may connect. Setting it
     * anyway restores the origin check, since an explicit list is the more specific instruction.
     *
     * This option only takes effect when `register-camel-restrictor` is `true` and a custom restrictor class
     * is not configured via `quarkus.camel.jolokia.additional-properties."restrictorClass"`.
     */
    Optional<List<String>> allowedOrigins();

    /**
     * When `true`, Jolokia stops refusing a request whose `Origin` uses `https` while the agent itself serves
     * plain `http`. That rule applies whatever `quarkus.camel.jolokia.allowed-origins` lists.
     *
     * Enable it only where the agent sits behind something that terminates TLS, since it otherwise allows a
     * page loaded over HTTPS to be served by an agent that is not.
     *
     * This is the equivalent of `<ignore-scheme/>` in the `<cors>` section of a Jolokia access policy, which is
     * not consulted.
     *
     * This option only takes effect when `register-camel-restrictor` is `true` and a custom restrictor class
     * is not configured via `quarkus.camel.jolokia.additional-properties."restrictorClass"`.
     */
    @WithDefault("false")
    boolean ignoreOriginScheme();

    interface Server {
        /**
         * Whether the Jolokia agent HTTP server should be started automatically.
         * When set to `false`, it is the user responsibility to start the server.
         * This can be done via `@Inject CamelQuarkusJolokiaServer` and then invoking the `start()` method.
         */
        @WithDefault("true")
        boolean autoStart();

        /**
         * The host address to which the Jolokia agent HTTP server should bind.
         * When unspecified, the default is localhost, except in remote dev mode, in dev and test mode on WSL,
         * and on Kubernetes with SSL client authentication configured, where it defaults to 0.0.0.0.
         */
        Optional<String> host();

        /**
         * The port on which the Jolokia agent HTTP server should listen.
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
         * The principal which must be given in a client certificate to allow access to Jolokia. For example
         * `cn=hawtio-online.hawtio.svc`.
         *
         * Without it, any client holding a certificate signed by the service CA is accepted. Setting it also
         * lets the default Camel Jolokia restrictor accept cross-origin requests forwarded by that client,
         * since the authenticated peer is then a known identity.
         */
        Optional<String> clientPrincipal();
    }
}
