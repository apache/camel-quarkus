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
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.quarkus.jolokia.config.JolokiaBuildTimeConfig;
import org.apache.camel.quarkus.jolokia.config.JolokiaRuntimeConfig;
import org.apache.camel.quarkus.jolokia.config.JolokiaRuntimeConfig.DiscoveryEnabledMode;
import org.apache.camel.quarkus.jolokia.config.JolokiaRuntimeConfig.Kubernetes;
import org.apache.camel.quarkus.jolokia.config.JolokiaRuntimeConfig.Server;
import org.apache.camel.quarkus.jolokia.restrictor.CamelJolokiaRestrictor;
import org.apache.camel.quarkus.jolokia.util.JolokiaHostUtils;
import org.apache.camel.quarkus.jolokia.util.JolokiaKubernetesUtils;
import org.apache.camel.util.CollectionHelper;
import org.apache.camel.util.ObjectHelper;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.jolokia.core.api.LogHandler;
import org.jolokia.core.util.ClassUtil;
import org.jolokia.jvmagent.JolokiaServer;
import org.jolokia.jvmagent.JolokiaServerConfig;
import org.jolokia.server.core.config.ConfigKey;

import static io.smallrye.common.os.Linux.isWSL;

@Recorder
public class JolokiaRecorder {
    private static final String ALL_INTERFACES = "0.0.0.0";
    private static final String LOCALHOST = "localhost";
    private static final String HOST = "host";
    private static final String PROTOCOL = "protocol";
    private static final String USE_SSL_CLIENT_AUTHENTICATION = "useSslClientAuthentication";
    private static final String CA_CERT = "caCert";
    private static final String CLIENT_PRINCIPAL = "clientPrincipal";
    private static final Logger LOG = Logger.getLogger(JolokiaRecorder.class);

    private final JolokiaBuildTimeConfig buildTimeConfig;
    private final RuntimeValue<JolokiaRuntimeConfig> runtimeConfig;

    public JolokiaRecorder(JolokiaBuildTimeConfig buildTimeConfig, RuntimeValue<JolokiaRuntimeConfig> runtimeConfig) {
        this.buildTimeConfig = buildTimeConfig;
        this.runtimeConfig = runtimeConfig;
    }

    public RuntimeValue<JolokiaServerConfig> createJolokiaServerConfig(String applicationName) {

        Server server = runtimeConfig.getValue().server();
        Kubernetes kubernetes = runtimeConfig.getValue().kubernetes();

        boolean onKubernetes = ConfigProvider.getConfig()
                .getOptionalValue("kubernetes.service.host", String.class)
                .isPresent();
        // Every client is authenticated by SSL client authentication in this case, so the agent can be reached
        // from the pod network without being exposed to anything unauthenticated
        boolean kubernetesClientAuthentication = JolokiaKubernetesUtils.isClientAuthenticationConfigured(kubernetes);

        // Configure Jolokia HTTP server host, port & context path
        String host = runtimeConfig.getValue().server().host().orElse(null);
        if (ObjectHelper.isEmpty(host)) {
            if (LaunchMode.current().isRemoteDev()) {
                host = ALL_INTERFACES;
            } else if (LaunchMode.current().isDevOrTest()) {
                if (!isWSL()) {
                    host = LOCALHOST;
                } else {
                    host = ALL_INTERFACES;
                }
            } else if (kubernetesClientAuthentication) {
                host = ALL_INTERFACES;
            } else {
                host = LOCALHOST;
            }
        }

        Map<String, String> serverOptions = new HashMap<>();
        serverOptions.put(HOST, host);
        serverOptions.put("port", String.valueOf(server.port()));
        serverOptions.put(ConfigKey.AGENT_CONTEXT.getKeyValue(), "/" + buildTimeConfig.path());

        // Attempt Kubernetes configuration
        if (kubernetesClientAuthentication) {
            serverOptions.put(ConfigKey.DISCOVERY_ENABLED.getKeyValue(), "false");
            serverOptions.put(PROTOCOL, "https");
            serverOptions.put(USE_SSL_CLIENT_AUTHENTICATION, "true");
            serverOptions.put("extendedClientCheck", "true");
            serverOptions.put(CA_CERT, kubernetes.serviceCaCert().getAbsolutePath());
            kubernetes.clientPrincipal().ifPresent(principal -> serverOptions.put(CLIENT_PRINCIPAL, principal));
        }

        // Merge configuration with any arbitrary values provided via quarkus.camel.jolokia.additional-properties
        Map<String, String> combinedOptions = CollectionHelper.mergeMaps(serverOptions,
                runtimeConfig.getValue().additionalProperties());

        // Check the merged options rather than the values set above, since additional-properties takes
        // precedence over both and can override the bind address and every option client authentication is
        // made of
        if (onKubernetes && kubernetes.clientAuthenticationEnabled()) {
            verifyKubernetesClientAuthentication(combinedOptions, kubernetes);
        }

        warnIfFetchMetadataDisabled(combinedOptions);

        // Configure CamelJolokiaRestrictor if an existing restrictor is not already provided
        if (runtimeConfig.getValue().registerCamelRestrictor()) {
            combinedOptions.putIfAbsent(ConfigKey.RESTRICTOR_CLASS.getKeyValue(), CamelJolokiaRestrictor.class.getName());
        }

        // Resolve the access policy before the server is created, since creating it binds the agent port
        if (isCamelRestrictor(combinedOptions.get(ConfigKey.RESTRICTOR_CLASS.getKeyValue()))) {
            CamelJolokiaRestrictor.verifyPolicyLocationResolvable();
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
            CamelQuarkusJolokiaAgent agent;
            if (serverConfig.getValue().getJolokiaConfig().containsKey(ConfigKey.LOGHANDLER_CLASS)) {
                agent = new CamelQuarkusJolokiaAgent(serverConfig.getValue());
            } else {
                agent = new CamelQuarkusJolokiaAgent(serverConfig.getValue(), new CamelQuarkusJolokiaLogHandler());
            }

            return new RuntimeValue<>(agent);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void startJolokiaServer(RuntimeValue<JolokiaServer> jolokiaServer) {
        if (runtimeConfig.getValue().server().autoStart()) {
            jolokiaServer.getValue().start();
        }
    }

    public void registerJolokiaServerShutdownHook(RuntimeValue<JolokiaServer> jolokiaServer, ShutdownContext shutdownContext) {
        shutdownContext.addShutdownTask(() -> jolokiaServer.getValue().stop());
    }

    public RuntimeValue<CamelQuarkusJolokiaServer> createJolokiaServerBean(RuntimeValue<JolokiaServer> jolokiaServer) {
        return new RuntimeValue<>(new CamelQuarkusJolokiaServer(jolokiaServer.getValue()));
    }

    /**
     * Fails the application when SSL client authentication is enabled on Kubernetes, the effective
     * configuration does not enforce it, and the agent is not confined to the loopback interface. That
     * combination would serve the agent to the pod network with no authentication at all.
     *
     * The options are the merged ones, so this holds however client authentication came to be disabled,
     * whether the service CA certificate is absent or `additional-properties` overrode one of the options
     * it is made of.
     */
    private static void verifyKubernetesClientAuthentication(Map<String, String> options, Kubernetes kubernetes) {
        if (isClientAuthenticationEnforced(options)) {
            if (ObjectHelper.isEmpty(options.get(CLIENT_PRINCIPAL))) {
                LOG.warn("Kubernetes SSL client authentication is enabled but no client principal is configured"
                        + " ('quarkus.camel.jolokia.kubernetes.client-principal'). Any pod presenting a valid"
                        + " certificate signed by the service CA can access Jolokia. Set a client principal"
                        + " to restrict access to a specific service identity. Until then cross-origin requests"
                        + " remain restricted to the origins listed by 'quarkus.camel.jolokia.allowed-origins',"
                        + " since the authenticated peer is not a known one.");
            }
            return;
        }

        // The two causes are not the same mistake, so they are not reported as one. An overridden option is a
        // working setup taken apart, while a missing service CA certificate means client authentication was never
        // on offer, and telling that operator to disable it would name something they never enabled
        boolean serviceCaCertPresent = kubernetes.serviceCaCert().exists();
        String reason = serviceCaCertPresent
                ? String.format("Kubernetes SSL client authentication is enabled but the effective Jolokia"
                        + " configuration does not enforce it. The '%s', '%s' or '%s' option has been overridden"
                        + " via 'quarkus.camel.jolokia.additional-properties'.",
                        PROTOCOL, USE_SSL_CLIENT_AUTHENTICATION, CA_CERT)
                : String.format("Jolokia SSL client authentication is not available on this cluster. There is no"
                        + " service CA certificate at '%s', which is written by the OpenShift service CA operator"
                        + " and is absent on plain Kubernetes.", kubernetes.serviceCaCert());

        String host = options.get(HOST);
        if (!isLoopbackHost(host)) {
            String remedy = serviceCaCertPresent
                    ? "Either correct that, set"
                            + " 'quarkus.camel.jolokia.kubernetes.client-authentication-enabled=false', or bind to"
                            + " localhost with 'quarkus.camel.jolokia.server.host=localhost'."
                    : "Set 'quarkus.camel.jolokia.kubernetes.client-authentication-enabled=false' to acknowledge"
                            + " that the agent authenticates nobody, bind to localhost with"
                            + " 'quarkus.camel.jolokia.server.host=localhost', or point"
                            + " 'quarkus.camel.jolokia.kubernetes.service-ca-cert' at a CA certificate you mount"
                            + " yourself.";
            throw new RuntimeException(String.format(
                    "%s Binding to '%s' would expose the agent without authentication. %s", reason, host, remedy));
        }

        LOG.warnf("%s Proceeding since the Jolokia agent is bound to '%s'.", reason, host);
    }

    /**
     * Warns when Jolokia's Fetch Metadata handling has been turned off on an agent that is reachable from off
     * the machine.
     *
     * A request carrying neither an Origin nor a Referer header is accepted, so that command line clients keep
     * working, and there is no equivalent of an access policy's &lt;strict-checking/&gt; to turn that off.
     * Jolokia's Sec-Fetch-* handling is what prevents a browser from making such a request on a visitor's
     * behalf, so disabling it leaves that path with nothing guarding it.
     *
     * This warns rather than fails, unlike the Kubernetes check above. That check reports a configuration
     * which asked for authentication and did not get it, whereas this one reports an explicit instruction to
     * switch a protection off, which is the operator's to give.
     */
    private static void warnIfFetchMetadataDisabled(Map<String, String> options) {
        String useFetchMetadata = options.get(ConfigKey.USE_FETCH_METADATA_HEADERS.getKeyValue());
        if (useFetchMetadata == null || Boolean.parseBoolean(useFetchMetadata)) {
            return;
        }

        String host = options.get(HOST);
        if (isLoopbackHost(host)) {
            return;
        }

        LOG.warnf("Jolokia Fetch Metadata handling is disabled via"
                + " 'quarkus.camel.jolokia.additional-properties.\"%s\"' while the agent is bound to '%s'."
                + " Requests carrying neither an Origin nor a Referer header are accepted, and the Sec-Fetch-*"
                + " headers are what prevent a browser from making one. Re-enable it unless the agent is"
                + " confined to the local machine.",
                ConfigKey.USE_FETCH_METADATA_HEADERS.getKeyValue(), host);
    }

    private static boolean isClientAuthenticationEnforced(Map<String, String> options) {
        return "https".equals(options.get(PROTOCOL))
                && Boolean.parseBoolean(options.get(USE_SSL_CLIENT_AUTHENTICATION))
                && ObjectHelper.isNotEmpty(options.get(CA_CERT));
    }

    /**
     * Whether the configured restrictor is `CamelJolokiaRestrictor` or a subclass of it.
     *
     * Comparing names would miss the documented way of customising the restrictor, and a subclass resolves the
     * access policy in the same inherited constructor, so it needs the same pre-check. Resolution goes through
     * Jolokia's own class lookup, which is what will load the class moments later. A name that does not resolve
     * is left alone, since Jolokia reports that itself when it creates the restrictor.
     */
    private static boolean isCamelRestrictor(String restrictorClassName) {
        if (ObjectHelper.isEmpty(restrictorClassName)) {
            return false;
        }
        Class<?> restrictorClass = ClassUtil.classForName(restrictorClassName);
        return restrictorClass != null && CamelJolokiaRestrictor.class.isAssignableFrom(restrictorClass);
    }

    static boolean isLoopbackHost(String host) {
        if (ObjectHelper.isEmpty(host)) {
            return false;
        }
        if (JolokiaHostUtils.isLoopbackAddress(host)) {
            return true;
        }
        // A literal was classified above, so only a name is resolved, recognising loopback aliases such as
        // ip6-localhost. Unlike a client supplied address, the bind host comes from configuration
        if (isAddressLiteral(host)) {
            return false;
        }

        try {
            return InetAddress.getByName(host).isLoopbackAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }

    /**
     * A colon appears only in an IPv6 literal, and digits and dots alone only in an IPv4 one. Neither is a name.
     */
    private static boolean isAddressLiteral(String host) {
        if (host.indexOf(':') != -1) {
            return true;
        }

        boolean dotted = false;
        for (int i = 0; i < host.length(); i++) {
            char c = host.charAt(i);
            if (c == '.') {
                dotted = true;
            } else if (c < '0' || c > '9') {
                return false;
            }
        }
        return dotted;
    }

    static final class CamelQuarkusJolokiaAgent extends JolokiaServer {
        CamelQuarkusJolokiaAgent(JolokiaServerConfig config) throws IOException {
            super(config);
        }

        CamelQuarkusJolokiaAgent(JolokiaServerConfig config, LogHandler logHandler) throws IOException {
            super(config, logHandler);
        }
    }
}
