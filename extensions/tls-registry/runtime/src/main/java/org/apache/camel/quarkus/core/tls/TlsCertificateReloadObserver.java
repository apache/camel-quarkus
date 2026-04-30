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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.quarkus.tls.CertificateUpdatedEvent;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.apache.camel.spi.ContextReloadStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Observes certificate update events from Quarkus TLS registry and triggers
 * Camel context reload to pick up new certificates.
 *
 * When a certificate is reloaded by Quarkus (e.g., file watch detects changes),
 * this observer:
 *
 * 1. Updates the corresponding SSLContextParameters bean in Camel registry
 * 2. Triggers a Camel context reload to restart routes with new certificates
 */
@ApplicationScoped
public class TlsCertificateReloadObserver {
    private static final Logger LOG = LoggerFactory.getLogger(TlsCertificateReloadObserver.class);

    @Inject
    CamelContext camelContext;

    @Inject
    TlsRegistryConfig config;

    private final AtomicReference<ScheduledFuture<?>> pendingReload = new AtomicReference<>();
    private ScheduledExecutorService scheduler;

    /**
     * Observes certificate updated events from Quarkus TLS registry.
     *
     * When a certificate is reloaded, this method:
     *
     * 1. Converts the updated TLS configuration to SSLContextParameters
     * 2. Updates the bean in Camel registry (or global SSL context)
     * 3. Triggers Camel context reload to restart routes
     *
     * @param event the certificate updated event
     */
    void onCertificateUpdated(@Observes CertificateUpdatedEvent event) {
        if (!config.enabled() || !config.reloadOnCertificateUpdate()) {
            LOG.debug("TLS registry integration is disabled, ignoring certificate update for '{}'", event.name());
            return;
        }

        LOG.info("Certificate '{}' has been updated, reloading Camel SSL configuration", event.name());

        try {
            // Access TLS registry to get the updated configuration
            TlsConfigurationRegistry tlsRegistry = TlsRegistryHelper.getTlsRegistry();
            if (tlsRegistry == null) {
                LOG.warn("TLS configuration registry not available, cannot reload certificate '{}'", event.name());
                return;
            }

            // Get the updated TLS configuration
            Optional<TlsConfiguration> tlsConfig = TlsRegistryHelper.getTlsConfiguration(tlsRegistry, event.name());
            if (tlsConfig.isEmpty()) {
                LOG.warn("TLS configuration '{}' not found in registry after update event", event.name());
                return;
            }

            // Update the bean in the Camel registry
            TlsRegistryHelper.registerOrUpdateBean(camelContext, config, event.name(), tlsConfig.get());

            // Schedule a debounced context reload to restart routes with new certificates
            scheduleCamelContextReload();
        } catch (Exception e) {
            LOG.error("Failed to update Camel SSL configuration for certificate '{}'", event.name(), e);
        }
    }

    /**
     * Schedules a debounced context reload.
     *
     * If multiple certificate updates occur in quick succession, this ensures only one
     * reload happens after the updates have stabilized.
     */
    private void scheduleCamelContextReload() {
        // Cancel any pending reload
        ScheduledFuture<?> existing = pendingReload.get();
        if (existing != null) {
            existing.cancel(false);
            LOG.debug("Canceled pending reload due to new certificate update");
        }

        // Schedule a new reload
        ScheduledFuture<?> newReload = getScheduler().schedule(
                this::triggerContextReload,
                config.reloadCertificateDelay(),
                TimeUnit.MILLISECONDS);

        pendingReload.set(newReload);
        LOG.debug("Scheduled context reload in {}ms", config.reloadCertificateDelay());
    }

    /**
     * Gets or creates the scheduler for debounced reloads.
     */
    private ScheduledExecutorService getScheduler() {
        if (scheduler == null) {
            scheduler = camelContext.getExecutorServiceManager()
                    .newScheduledThreadPool(this, "CamelQuarkusTlsReload", 1);
        }
        return scheduler;
    }

    /**
     * Cleanup scheduler on bean destruction.
     */
    @PreDestroy
    void cleanup() {
        ScheduledFuture<?> pending = pendingReload.get();
        if (pending != null) {
            pending.cancel(false);
        }
        if (scheduler != null) {
            camelContext.getExecutorServiceManager().shutdown(scheduler);
        }
    }

    /**
     * Triggers a Camel context reload to restart routes with updated certificates.
     *
     * This uses Camel's context reload strategy to gracefully restart routes without
     * stopping the entire application.
     */
    private void triggerContextReload() {
        try {
            ContextReloadStrategy reloadStrategy = camelContext.hasService(ContextReloadStrategy.class);
            if (reloadStrategy != null) {
                LOG.info("Triggering Camel context reload to apply updated certificates");
                reloadStrategy.onReload(this);
            } else {
                LOG.warn("ContextReloadStrategy not available, routes will not be restarted. "
                        + "New certificates will be used on next route start.");
            }
        } catch (Exception e) {
            LOG.error("Failed to trigger Camel context reload", e);
        }
    }
}
