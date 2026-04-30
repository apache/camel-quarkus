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

import java.util.Optional;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.quarkus.tls.runtime.config.TlsConfig;
import org.apache.camel.CamelContext;
import org.apache.camel.spi.ContextReloadStrategy;
import org.apache.camel.support.DefaultContextReloadStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runtime recorder for registering Quarkus TLS configurations as Camel SSLContextParameters beans.
 */
@Recorder
public class TlsRegistryRecorder {
    private static final Logger LOG = LoggerFactory.getLogger(TlsRegistryRecorder.class);

    private final RuntimeValue<TlsRegistryConfig> tlsRegistryConfigRuntimeValue;
    private final RuntimeValue<TlsConfig> tlsConfigRuntimeValue;

    public TlsRegistryRecorder(RuntimeValue<TlsRegistryConfig> tlsRegistryConfigRuntimeValue,
            RuntimeValue<TlsConfig> tlsConfigRuntimeValue) {
        this.tlsRegistryConfigRuntimeValue = tlsRegistryConfigRuntimeValue;
        this.tlsConfigRuntimeValue = tlsConfigRuntimeValue;
    }

    /**
     * Register Quarkus TLS configurations as Camel SSLContextParameters beans.
     * Called at RUNTIME_INIT after the Camel context is created.
     *
     * @param camelContextRuntimeValue the Camel context
     */
    public void registerTlsConfigurations(RuntimeValue<CamelContext> camelContextRuntimeValue) {
        TlsRegistryConfig config = tlsRegistryConfigRuntimeValue.getValue();

        if (!config.enabled()) {
            LOG.debug("TLS registry integration is disabled");
            return;
        }

        // Access TLS registry at runtime
        TlsConfigurationRegistry tlsRegistry = TlsRegistryHelper.getTlsRegistry();
        if (tlsRegistry == null) {
            LOG.debug("TLS configuration registry not available (no TLS configured)");
            return;
        }

        CamelContext camelContext = camelContextRuntimeValue.getValue();

        // Configure context reloading for cert updates if required
        if (config.reloadOnCertificateUpdate() && camelContext.hasService(ContextReloadStrategy.class) == null) {
            ContextReloadStrategy reloader = new DefaultContextReloadStrategy();
            try {
                camelContext.addService(reloader);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        // Process default TLS configuration
        Optional<TlsConfiguration> defaultTls = tlsRegistry.getDefault();
        if (defaultTls.isPresent()) {
            TlsRegistryHelper.registerBean(camelContext, config, "default", defaultTls.get());
        }

        // Process all named TLS configurations
        TlsConfig tlsConfig = tlsConfigRuntimeValue.getValue();
        for (String name : tlsConfig.namedCertificateConfig().keySet()) {
            Optional<TlsConfiguration> namedTls = tlsRegistry.get(name);
            if (namedTls.isPresent()) {
                LOG.debug("Processing named TLS configuration '{}'", name);
                TlsRegistryHelper.registerBean(camelContext, config, name, namedTls.get());
            }
        }
    }
}
