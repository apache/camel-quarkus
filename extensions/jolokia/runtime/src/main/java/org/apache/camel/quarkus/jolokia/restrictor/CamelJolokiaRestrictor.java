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
package org.apache.camel.quarkus.jolokia.restrictor;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import javax.management.ObjectName;

import io.smallrye.config.SmallRyeConfig;
import org.apache.camel.quarkus.jolokia.config.JolokiaBuildTimeConfig;
import org.apache.camel.quarkus.jolokia.config.JolokiaRuntimeConfig;
import org.apache.camel.quarkus.jolokia.util.JolokiaHostUtils;
import org.apache.camel.quarkus.jolokia.util.JolokiaKubernetesUtils;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.jolokia.server.core.config.ConfigKey;
import org.jolokia.server.core.restrictor.DenyAllRestrictor;
import org.jolokia.server.core.restrictor.RestrictorFactory;
import org.jolokia.server.core.restrictor.policy.PolicyRestrictor;
import org.jolokia.server.core.service.api.Restrictor;
import org.jolokia.server.core.util.HttpMethod;
import org.jolokia.server.core.util.RequestType;

/**
 * Restricts Jolokia access to the configured MBean domains, on top of a delegate that decides everything else.
 *
 * The delegate is built once, so that each access mode is expressed in one place rather than as flags tested by
 * every check. Client addresses and origins are answered independently, so asking for one never withdraws the
 * other:
 *
 * <ul>
 * <li>an access policy that could not be loaded denies everything, and one that was explicitly configured but
 * does not resolve fails the application at startup</li>
 * <li>by default access is restricted to loopback clients, plus the origins listed by `allowed-origins`</li>
 * <li>`remote-access-allowed` lifts the address restriction and nothing else, so cross-origin requests stay
 * restricted whether or not it is set</li>
 * <li>Kubernetes SSL client authentication lifts the address restriction too, since the transport has already
 * authenticated the client. It lifts the origin restriction as well only once `client-principal` pins which
 * identity may connect, and only while `allowed-origins` lists nothing</li>
 * <li>a loaded access policy narrows what is left through its `&lt;commands&gt;`, `&lt;http&gt;` and MBean
 * rules. It decides client addresses outright when it carries a `&lt;remote&gt;` section, since those rules are
 * how operators are expected to restrict addresses; a policy without one says nothing about addresses and
 * leaves the rules above to answer. Its `&lt;cors&gt;` section is not consulted at all, since origins are
 * configured by `allowed-origins` and the scheme rule by `ignore-origin-scheme`</li>
 * </ul>
 *
 * The checks themselves are final. A subclass restricts further through the `allows*` hooks, which are asked
 * only once the inherited checks have allowed the request, so extending this class can narrow what it permits
 * but never widen it.
 */
public class CamelJolokiaRestrictor implements Restrictor {
    private static final Logger LOG = Logger.getLogger(CamelJolokiaRestrictor.class);

    private final Set<String> ALLOWED_DOMAINS = Collections.unmodifiableSet(
            ConfigProvider.getConfig()
                    .unwrap(SmallRyeConfig.class)
                    .getConfigMapping(JolokiaBuildTimeConfig.class)
                    .camelRestrictorAllowedMbeanDomains());

    private final boolean ignoreOriginScheme = runtimeConfig().ignoreOriginScheme();

    private final Restrictor delegate;

    public CamelJolokiaRestrictor() {
        this.delegate = createDelegate();
    }

    @Override
    public final boolean isRemoteAccessAllowed(String... hostOrAddress) {
        return delegate.isRemoteAccessAllowed(hostOrAddress) && allowsRemoteAccess(hostOrAddress);
    }

    @Override
    public final boolean isOriginAllowed(String origin, boolean strictCheck) {
        return delegate.isOriginAllowed(origin, strictCheck) && allowsOrigin(origin, strictCheck);
    }

    @Override
    public final boolean isHttpMethodAllowed(HttpMethod method) {
        return delegate.isHttpMethodAllowed(method) && allowsHttpMethod(method);
    }

    @Override
    public final boolean isTypeAllowed(RequestType type) {
        return delegate.isTypeAllowed(type) && allowsRequestType(type);
    }

    @Override
    public final boolean ignoreScheme() {
        return ignoreOriginScheme;
    }

    @Override
    public final boolean isAttributeReadAllowed(ObjectName objectName, String attribute) {
        return isAllowedDomain(objectName)
                && delegate.isAttributeReadAllowed(objectName, attribute)
                && allowsAttributeRead(objectName, attribute);
    }

    @Override
    public final boolean isAttributeWriteAllowed(ObjectName objectName, String attribute) {
        return isAllowedDomain(objectName)
                && delegate.isAttributeWriteAllowed(objectName, attribute)
                && allowsAttributeWrite(objectName, attribute);
    }

    @Override
    public final boolean isOperationAllowed(ObjectName objectName, String operation) {
        return isAllowedDomain(objectName)
                && delegate.isOperationAllowed(objectName, operation)
                && allowsOperation(objectName, operation);
    }

    @Override
    public final boolean isObjectNameHidden(ObjectName objectName) {
        return !isAllowedDomain(objectName)
                || delegate.isObjectNameHidden(objectName)
                || !allowsObjectName(objectName);
    }

    /**
     * Whether a client address chain the inherited checks accepted may reach the agent.
     */
    protected boolean allowsRemoteAccess(String... hostOrAddress) {
        return true;
    }

    /**
     * Whether an origin the inherited checks accepted may reach the agent.
     */
    protected boolean allowsOrigin(String origin, boolean strictCheck) {
        return true;
    }

    /**
     * Whether an HTTP method the inherited checks accepted may be used.
     */
    protected boolean allowsHttpMethod(HttpMethod method) {
        return true;
    }

    /**
     * Whether a request type the inherited checks accepted may be used.
     */
    protected boolean allowsRequestType(RequestType type) {
        return true;
    }

    /**
     * Whether an attribute the inherited checks accepted may be read.
     */
    protected boolean allowsAttributeRead(ObjectName objectName, String attribute) {
        return true;
    }

    /**
     * Whether an attribute the inherited checks accepted may be written.
     */
    protected boolean allowsAttributeWrite(ObjectName objectName, String attribute) {
        return true;
    }

    /**
     * Whether an operation the inherited checks accepted may be invoked.
     */
    protected boolean allowsOperation(ObjectName objectName, String operation) {
        return true;
    }

    /**
     * Whether an MBean the inherited checks left visible may be listed. Returning `false` hides it, keeping the
     * `false` means less access reading of every other hook.
     */
    protected boolean allowsObjectName(ObjectName objectName) {
        return true;
    }

    /**
     * Whether the MBean is in one of the domains `camel-restrictor-allowed-mbean-domains` lists.
     *
     * Applied by the checks above already. Exposed so that a hook can consult it, not so that it can be
     * reapplied.
     */
    protected final boolean isAllowedDomain(ObjectName objectName) {
        return ALLOWED_DOMAINS.contains(objectName.getDomain());
    }

    /**
     * Fails the application when an explicitly configured access policy cannot be resolved.
     *
     * This runs while the Jolokia server configuration is assembled, before the server itself is created. The
     * server binds the agent port in its constructor and only releases it once `start()` has completed, so
     * throwing later, as the restrictor is built during `start()`, would leave the port bound for the lifetime
     * of the JVM.
     */
    public static void verifyPolicyLocationResolvable() {
        JolokiaRuntimeConfig config = runtimeConfig();
        if (!config.additionalProperties().containsKey(ConfigKey.POLICY_LOCATION.getKeyValue())) {
            return;
        }

        String location = policyLocation(config);
        PolicyRestrictor policy;
        try {
            policy = RestrictorFactory.lookupPolicyRestrictor(location);
        } catch (MalformedURLException | FileNotFoundException e) {
            // Nothing is there. A `classpath:` location reports that by returning null below, while every other
            // scheme raises one of these, so the two are the same packaging error and fail the same way
            throw unresolvablePolicy(location);
        } catch (Exception e) {
            // A policy that resolves but cannot be read denies all access instead of failing startup, which
            // createDelegate() reports when the restrictor is built
            return;
        }

        if (policy == null) {
            throw unresolvablePolicy(location);
        }
    }

    private static Restrictor createDelegate() {
        JolokiaRuntimeConfig config = runtimeConfig();
        String location = policyLocation(config);

        PolicyRestrictor policy;
        try {
            policy = RestrictorFactory.lookupPolicyRestrictor(location);
        } catch (Exception e) {
            LOG.errorf(e, "Failed to load the Jolokia access policy from %s. Denying all Jolokia access", location);
            return new DenyAllRestrictor();
        }

        if (policy == null && config.additionalProperties().containsKey(ConfigKey.POLICY_LOCATION.getKeyValue())) {
            throw unresolvablePolicy(location);
        }

        List<Pattern> allowedOrigins = allowedOrigins(config);
        boolean clientAuthenticated = JolokiaKubernetesUtils.isClientAuthenticationConfigured(config.kubernetes());

        boolean anyAddressAllowed = config.remoteAccessAllowed() || clientAuthenticated;

        // An explicit origin list is the more specific instruction, so it wins
        boolean anyOriginAllowed = clientAuthenticated
                && config.kubernetes().clientPrincipal().isPresent()
                && allowedOrigins.isEmpty();

        LoopbackRestrictor restrictor = new LoopbackRestrictor(policy, allowedOrigins, anyAddressAllowed,
                anyOriginAllowed);

        // The policy is loaded here rather than by RestrictorFactory, so nothing else reports that one was found
        LOG.infof("Jolokia %s. Client addresses: %s. Origins: %s",
                policy == null
                        ? "found no access policy at " + location
                        : "loaded the access policy " + location,
                addressSourceDescription(config, restrictor, anyAddressAllowed),
                originSourceDescription(allowedOrigins, anyOriginAllowed));

        return restrictor;
    }

    private static String addressSourceDescription(JolokiaRuntimeConfig config, LoopbackRestrictor restrictor,
            boolean anyAddressAllowed) {
        if (restrictor.policyDecidesAddresses) {
            return "decided by the access policy <remote> section";
        }
        if (anyAddressAllowed) {
            return config.remoteAccessAllowed()
                    ? "any, since remote-access-allowed is set"
                    : "any, since Kubernetes SSL client authentication verifies every client";
        }
        return "loopback only";
    }

    private static String originSourceDescription(List<Pattern> allowedOrigins, boolean anyOriginAllowed) {
        if (anyOriginAllowed) {
            return "any, since a Kubernetes client principal pins which identity may connect";
        }
        if (allowedOrigins.isEmpty()) {
            return "loopback only";
        }
        return "loopback plus the " + allowedOrigins.size()
                + (allowedOrigins.size() == 1 ? " origin" : " origins") + " listed by allowed-origins";
    }

    private static List<Pattern> allowedOrigins(JolokiaRuntimeConfig config) {
        return config.allowedOrigins()
                .orElseGet(List::of)
                .stream()
                .map(origin -> origin.trim().toLowerCase(Locale.ROOT))
                .filter(origin -> !origin.isEmpty())
                .map(CamelJolokiaRestrictor::originPattern)
                .toList();
    }

    /**
     * Compiles a configured origin the same way Jolokia compiles an `&lt;allow-origin&gt;` rule, so that the
     * values of a policy file can be moved across unchanged and `*` keeps the meaning it has there.
     */
    private static Pattern originPattern(String origin) {
        return Pattern.compile("^" + Pattern.quote(origin).replace("*", "\\E.*\\Q") + "$");
    }

    private static JolokiaRuntimeConfig runtimeConfig() {
        return ConfigProvider.getConfig()
                .unwrap(SmallRyeConfig.class)
                .getConfigMapping(JolokiaRuntimeConfig.class);
    }

    private static IllegalStateException unresolvablePolicy(String location) {
        return new IllegalStateException(String.format(
                "The configured Jolokia access policy '%s' could not be resolved."
                        + " A 'classpath:' location in a native executable has to be registered with"
                        + " 'quarkus.native.resources.includes'. Any other location has to exist, so check that"
                        + " a mounted file is where it is expected. Otherwise correct or remove"
                        + " 'quarkus.camel.jolokia.additional-properties.\"policyLocation\"'.",
                location));
    }

    private static String policyLocation(JolokiaRuntimeConfig config) {
        return config.additionalProperties()
                .getOrDefault(ConfigKey.POLICY_LOCATION.getKeyValue(), ConfigKey.POLICY_LOCATION.getDefaultValue());
    }

    /**
     * Allows loopback clients, or every client when `remote-access-allowed` is set, plus the configured origins,
     * narrowed by an optional Jolokia access policy.
     */
    static final class LoopbackRestrictor implements Restrictor {
        /**
         * TEST-NET-1, reserved for documentation by RFC 5737, so no policy lists it to grant anyone access.
         * Asking a policy about it reveals whether it restricts addresses at all.
         */
        private static final String UNROUTABLE_PROBE_ADDRESS = "192.0.2.1";

        private final PolicyRestrictor policy;
        private final List<Pattern> allowedOrigins;
        private final boolean anyAddressAllowed;
        private final boolean anyOriginAllowed;
        private final boolean policyDecidesAddresses;

        LoopbackRestrictor(PolicyRestrictor policy, List<Pattern> allowedOrigins, boolean anyAddressAllowed,
                boolean anyOriginAllowed) {
            this.policy = policy;
            this.allowedOrigins = allowedOrigins;
            this.anyAddressAllowed = anyAddressAllowed;
            this.anyOriginAllowed = anyOriginAllowed;
            // A policy listing 0.0.0.0/0 reads the same as one with no `<remote>` section, which is the
            // stricter reading of the two. Jolokia exposes no way to ask about the section directly
            this.policyDecidesAddresses = policy != null && !policy.isRemoteAccessAllowed(UNROUTABLE_PROBE_ADDRESS);
        }

        /**
         * A policy that restricts addresses answers this on its own. Its `&lt;remote&gt;` rules are the mechanism
         * operators are expected to restrict addresses with, so exempting loopback from them would let the
         * framework widen a restriction that was asked for explicitly.
         *
         * A policy with no `&lt;remote&gt;` section says nothing about addresses, so the configured rules answer
         * instead. Letting Jolokia's reading of an absent section apply here would mean a policy brought to
         * restrict something else, such as the commands in use, silently withdrew the loopback default and left
         * `remote-access-allowed` reading as though it still applied.
         */
        @Override
        public boolean isRemoteAccessAllowed(String... hostOrAddress) {
            if (policyDecidesAddresses) {
                return policy.isRemoteAccessAllowed(hostOrAddress);
            }
            return anyAddressAllowed || isLoopbackChain(hostOrAddress);
        }

        /**
         * Requests carrying no Origin header, loopback origins, and the origins listed by `allowed-origins` are
         * permitted. Every other origin is denied, so that a browser on another site cannot drive the agent.
         *
         * The value is reduced to a scheme, host and port before being matched. Jolokia falls back to the
         * Referer header when there is no Origin, and a Referer carries a path, so comparing what arrives
         * verbatim would refuse a request from an origin that is listed. A default port is matched whether or
         * not the configured value spells it out, since the two are the same origin.
         *
         * The `&lt;cors&gt;` section of an access policy is deliberately not consulted, for either this check or
         * the Access-Control-Allow-Origin response header, so that the origins which may reach the agent are
         * configured in one place. Reading `&lt;allow-origin&gt;` as an allowlist would give it a meaning it does
         * not have on a plain Jolokia agent, where it does not grant access, and honouring its actual meaning
         * would require the same origin to be listed both here and in the policy. `strictCheck` is unused for the
         * same reason: it selects between those two policy behaviours.
         */
        @Override
        public boolean isOriginAllowed(String origin, boolean strictCheck) {
            if (anyOriginAllowed || origin == null) {
                return true;
            }

            URI uri;
            try {
                uri = new URI(origin.trim());
            } catch (URISyntaxException e) {
                return false;
            }

            // An origin is a scheme, host and port. User information in particular never appears in one, so its
            // presence is an attempt to make a foreign host read as an allowed one
            if (uri.getScheme() == null || uri.getHost() == null || uri.getUserInfo() != null) {
                return false;
            }

            if (JolokiaHostUtils.isLoopbackAddress(uri.getHost())) {
                return true;
            }

            for (String requestOrigin : originSpellings(uri)) {
                for (Pattern allowedOrigin : allowedOrigins) {
                    if (allowedOrigin.matcher(requestOrigin).matches()) {
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * Renders the scheme, host and port of the given URI as an origin, both without and with the default
         * port for the scheme.
         *
         * A browser leaves that port out, while a value copied from an address bar or from the
         * `&lt;allow-origin&gt;` rules of an access policy may spell it out. The two denote the same origin, so
         * both are matched and either may be configured.
         */
        private static List<String> originSpellings(URI uri) {
            String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
            String host = uri.getHost().toLowerCase(Locale.ROOT);
            int port = uri.getPort();
            int defaultPort = defaultPort(scheme);
            if (port != -1 && port != defaultPort) {
                return List.of(scheme + "://" + host + ":" + port);
            }

            String origin = scheme + "://" + host;
            return defaultPort == -1 ? List.of(origin) : List.of(origin, origin + ":" + defaultPort);
        }

        private static int defaultPort(String scheme) {
            switch (scheme) {
            case "http":
                return 80;
            case "https":
                return 443;
            default:
                return -1;
            }
        }

        @Override
        public boolean isHttpMethodAllowed(HttpMethod method) {
            return policy == null || policy.isHttpMethodAllowed(method);
        }

        @Override
        public boolean isTypeAllowed(RequestType type) {
            return policy == null || policy.isTypeAllowed(type);
        }

        /**
         * Never consulted, since the enclosing restrictor answers this from configuration. Declared rather than
         * inherited so that the interface default cannot quietly become the answer.
         */
        @Override
        public boolean ignoreScheme() {
            return false;
        }

        @Override
        public boolean isAttributeReadAllowed(ObjectName objectName, String attribute) {
            return policy == null || policy.isAttributeReadAllowed(objectName, attribute);
        }

        @Override
        public boolean isAttributeWriteAllowed(ObjectName objectName, String attribute) {
            return policy == null || policy.isAttributeWriteAllowed(objectName, attribute);
        }

        @Override
        public boolean isOperationAllowed(ObjectName objectName, String operation) {
            return policy == null || policy.isOperationAllowed(objectName, operation);
        }

        @Override
        public boolean isObjectNameHidden(ObjectName objectName) {
            return policy != null && policy.isObjectNameHidden(objectName);
        }

        private static boolean isLoopbackChain(String... hostOrAddress) {
            if (hostOrAddress == null || hostOrAddress.length == 0) {
                return false;
            }
            for (String addr : hostOrAddress) {
                if (!JolokiaHostUtils.isLoopbackAddress(addr)) {
                    return false;
                }
            }
            return true;
        }
    }
}
