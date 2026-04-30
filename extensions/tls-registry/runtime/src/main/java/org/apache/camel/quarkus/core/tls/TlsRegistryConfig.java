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
package org.apache.camel.quarkus.core.tls;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * Runtime configuration for the Camel Quarkus TLS Registry integration.
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "quarkus.camel.tls-registry")
public interface TlsRegistryConfig {

    String DEFAULT_BEAN_NAME = "defaultSslContextParameters";

    /**
     * Enable automatic conversion and registration of Quarkus TLS configurations
     * as Camel SSLContextParameters beans.
     *
     * When enabled, the default TLS configuration and any named configurations will
     * be automatically discovered and registered as beans in the Camel registry.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * Whether to set the Quarkus default TLS configuration as Camel's global
     * SSLContextParameters.
     *
     * When true, if a default TLS configuration exists (`quarkus.tls.*`), it will
     * be converted and set as the global SSL context via `CamelContext.setSSLContextParameters()`.
     * Components can opt in to this global context using `useGlobalSslContextParameters=true`.
     */
    @WithName("quarkus-default-as-global")
    @WithDefault("false")
    boolean quarkusDefaultAsGlobal();

    /**
     * The name to use when registering the default TLS configuration as a bean.
     *
     * Only applicable if the default configuration is not set as global
     * (i.e., `quarkus-default-as-global=false`).
     */
    @WithDefault(DEFAULT_BEAN_NAME)
    String defaultBeanName();

    /**
     * Enable automatic Camel Context reload when certificates are updated.
     *
     * When enabled, if Quarkus reloads a certificate (e.g., file watch detects changes),
     * the corresponding SSLContextParameters bean will be updated in the Camel registry
     * and a context reload will be triggered to restart routes with the new certificates.
     *
     * This uses Camel's `ContextReloadStrategy` to gracefully restart routes without
     * stopping the entire application.
     *
     * To avoid excessive reloads when multiple certificates are updated in quick succession,
     * the reload is debounced with a configurable delay. If additional certificate updates occur
     * during this delay, the timer is reset, ensuring only one reload happens after all
     * updates have stabilized.
     */
    @WithDefault("true")
    boolean reloadOnCertificateUpdate();

    /**
     * Delay period to avoid excessive Camel Context reloads when multiple certificates are updated in quick succession.
     */
    @WithDefault("2000")
    long reloadCertificateDelay();
}
