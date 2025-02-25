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
package org.apache.camel.quarkus.jolokia;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;
import org.apache.camel.quarkus.jolokia.config.JolokiaRuntimeConfig;
import org.apache.camel.quarkus.jolokia.config.JolokiaRuntimeConfig.DiscoveryEnabledMode;
import org.apache.camel.quarkus.jolokia.config.JolokiaRuntimeConfig.Kubernetes;
import org.apache.camel.quarkus.jolokia.config.JolokiaRuntimeConfig.Server;
import org.apache.camel.quarkus.jolokia.restrictor.CamelJolokiaRestrictor;
import org.apache.camel.util.CollectionHelper;
import org.apache.camel.util.HostUtils;
import org.apache.camel.util.ObjectHelper;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.jolokia.jvmagent.JolokiaServer;
import org.jolokia.jvmagent.JolokiaServerConfig;
import org.jolokia.server.core.config.ConfigKey;

import static io.smallrye.common.os.Linux.isWSL;

@Recorder
public class JolokiaRecorder {
    private static final String ALL_INTERFACES = "0.0.0.0";
    private static final String LOCALHOST = "localhost";
    private static final Logger LOG = Logger.getLogger(JolokiaRequestRedirectHandler.class);

    public Consumer<Route> route(Handler<RoutingContext> bodyHandler) {
        return new Consumer<Route>() {
            @Override
            public void accept(Route route) {
                route.handler(bodyHandler).produces("application/json");
            }
        };
    }

    public RuntimeValue<JolokiaServerConfig> createJolokiaServerConfig(
            JolokiaRuntimeConfig runtimeConfig,
            String endpointPath,
            String applicationName) {

        Server server = runtimeConfig.server();
        Kubernetes kubernetes = runtimeConfig.kubernetes();

        // Configure Jolokia HTTP server host, port & context path
        String host = runtimeConfig.server().host().orElse(null);
        if (ObjectHelper.isEmpty(host)) {
            if (LaunchMode.isRemoteDev()) {
                host = ALL_INTERFACES;
            } else if (LaunchMode.current().isDevOrTest()) {
                if (!isWSL()) {
                    host = LOCALHOST;
                } else {
                    host = ALL_INTERFACES;
                }
            } else {
                host = ALL_INTERFACES;
            }
        }

        Map<String, String> serverOptions = new HashMap<>();
        serverOptions.put("host", host);
        serverOptions.put("port", String.valueOf(server.port()));
        serverOptions.put(ConfigKey.AGENT_CONTEXT.getKeyValue(), "/" + endpointPath);

        // Attempt Kubernetes configuration
        Optional<String> kubernetesServiceHost = ConfigProvider.getConfig().getOptionalValue("kubernetes.service.host",
                String.class);
        if (kubernetesServiceHost.isPresent()) {
            if (kubernetes.clientAuthenticationEnabled() && kubernetes.serviceCaCert().exists()) {
                serverOptions.put(ConfigKey.DISCOVERY_ENABLED.getKeyValue(), "false");
                serverOptions.put("protocol", "https");
                serverOptions.put("useSslClientAuthentication", "true");
                serverOptions.put("extendedClientCheck", "true");
                serverOptions.put("caCert", kubernetes.serviceCaCert().getAbsolutePath());
                kubernetes.clientPrincipal()
                        .ifPresent(clientPrincipal -> serverOptions.put("clientPrincipal", clientPrincipal));
            } else {
                LOG.warnf("Kubernetes service CA certificate %s does not exist", kubernetes.serviceCaCert());
            }
        }

        // Merge configuration with any arbitrary values provided via quarkus.camel.jolokia.additional-properties
        Map<String, String> combinedOptions = CollectionHelper.mergeMaps(serverOptions,
                runtimeConfig.additionalProperties());

        // Configure CamelJolokiaRestrictor if an existing restrictor is not already provided
        if (runtimeConfig.registerCamelRestrictor()) {
            combinedOptions.putIfAbsent(ConfigKey.RESTRICTOR_CLASS.getKeyValue(), CamelJolokiaRestrictor.class.getName());
        }

        // Enable discovery based on the provided mode
        DiscoveryEnabledMode discoveryMode = server.discoveryEnabledMode();
        if (discoveryMode != DiscoveryEnabledMode.NONE) {
            if ((discoveryMode == DiscoveryEnabledMode.ALL)
                    || (discoveryMode == DiscoveryEnabledMode.DEV_TEST && LaunchMode.current().isDevOrTest())) {
                combinedOptions.putIfAbsent(ConfigKey.DISCOVERY_ENABLED.getKeyValue(), "true");
            }
        }

        // Set a default agent description so that it shows up during agent discovery
        combinedOptions.putIfAbsent(ConfigKey.AGENT_DESCRIPTION.getKeyValue(), applicationName);

        return new RuntimeValue<>(new JolokiaServerConfig(combinedOptions));
    }

    public RuntimeValue<JolokiaServer> createJolokiaServer(RuntimeValue<JolokiaServerConfig> serverConfig) {
        try {
            CamelQuarkusJolokiaAgent agent = new CamelQuarkusJolokiaAgent(serverConfig.getValue());
            return new RuntimeValue<>(agent);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void startJolokiaServer(RuntimeValue<JolokiaServer> jolokiaServer, JolokiaRuntimeConfig config) {
        if (config.server().autoStart()) {
            jolokiaServer.getValue().start();
        }
    }

    public void registerJolokiaServerShutdownHook(RuntimeValue<JolokiaServer> jolokiaServer, ShutdownContext shutdownContext) {
        shutdownContext.addShutdownTask(() -> jolokiaServer.getValue().stop());
    }

    public RuntimeValue<CamelQuarkusJolokiaServer> createJolokiaServerBean(RuntimeValue<JolokiaServer> jolokiaServer) {
        return new RuntimeValue<>(new CamelQuarkusJolokiaServer(jolokiaServer.getValue()));
    }

    static final class CamelQuarkusJolokiaAgent extends JolokiaServer {
        CamelQuarkusJolokiaAgent(JolokiaServerConfig config) throws IOException {
            super(config, new CamelQuarkusJolokiaLogHandler());
        }
    }

    public Handler<RoutingContext> getHandler(RuntimeValue<JolokiaServerConfig> config, String jolokiaEndpointPath) {
        JolokiaServerConfig serverConfig = config.getValue();
        String host = resolveHost(serverConfig.getAddress());
        URI uri = URI.create("%s://%s:%d%s".formatted(serverConfig.getProtocol(), host, serverConfig.getPort(),
                serverConfig.getContextPath()));
        return new JolokiaRequestRedirectHandler(uri.normalize(), jolokiaEndpointPath);
    }

    static String resolveHost(InetAddress address) {
        if (address == null) {
            try {
                return HostUtils.getLocalHostName();
            } catch (UnknownHostException e) {
                throw new IllegalStateException("Unable to determine the Jolokia host", e);
            }
        }
        return address.getHostName();
    }
}
