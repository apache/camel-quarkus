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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Set;

import javax.management.ObjectName;

import io.smallrye.config.SmallRyeConfig;
import org.apache.camel.quarkus.jolokia.config.JolokiaBuildTimeConfig;
import org.apache.camel.quarkus.jolokia.util.JolokiaHostUtils;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.jolokia.server.core.restrictor.RestrictorFactory;
import org.jolokia.server.core.restrictor.policy.PolicyRestrictor;
import org.jolokia.server.core.service.api.Restrictor;
import org.jolokia.server.core.util.HttpMethod;
import org.jolokia.server.core.util.RequestType;

public class CamelJolokiaRestrictor implements Restrictor {
    private static final String DEFAULT_POLICY_LOCATION = "classpath:/jolokia-access.xml";
    private static final Logger LOG = Logger.getLogger(CamelJolokiaRestrictor.class);

    private final Set<String> ALLOWED_DOMAINS = Collections.unmodifiableSet(
            ConfigProvider.getConfig()
                    .unwrap(SmallRyeConfig.class)
                    .getConfigMapping(JolokiaBuildTimeConfig.class)
                    .camelRestrictorAllowedMbeanDomains());

    private final boolean remoteAccessAllowed = ConfigProvider.getConfig()
            .getOptionalValue("quarkus.camel.jolokia.remote-access-allowed", Boolean.class)
            .orElse(false);

    private final PolicyRestrictor policyRestrictor;

    public CamelJolokiaRestrictor() {
        this.policyRestrictor = loadPolicyRestrictor();
    }

    @Override
    public boolean isRemoteAccessAllowed(String... hostOrAddress) {
        if (remoteAccessAllowed) {
            return true;
        }
        if (hostOrAddress != null) {
            for (String addr : hostOrAddress) {
                if (isLoopbackAddress(addr)) {
                    return true;
                }
            }
        }
        if (policyRestrictor != null) {
            return policyRestrictor.isRemoteAccessAllowed(hostOrAddress);
        }
        return false;
    }

    @Override
    public boolean isHttpMethodAllowed(HttpMethod method) {
        if (policyRestrictor != null) {
            return policyRestrictor.isHttpMethodAllowed(method);
        }
        return true;
    }

    @Override
    public boolean isTypeAllowed(RequestType type) {
        if (policyRestrictor != null) {
            return policyRestrictor.isTypeAllowed(type);
        }
        return true;
    }

    @Override
    public boolean isOriginAllowed(String origin, boolean strictCheck) {
        if (origin == null || remoteAccessAllowed) {
            return true;
        }
        try {
            String host = new URI(origin).getHost();
            if (isLoopbackAddress(host)) {
                return true;
            }
        } catch (URISyntaxException e) {
            return false;
        }
        if (policyRestrictor != null) {
            return policyRestrictor.isOriginAllowed(origin, strictCheck);
        }
        return false;
    }

    @Override
    public boolean ignoreScheme() {
        if (policyRestrictor != null) {
            return policyRestrictor.ignoreScheme();
        }
        return false;
    }

    @Override
    public boolean isAttributeReadAllowed(ObjectName objectName, String attribute) {
        return isAllowedDomain(objectName) && isTypeAllowed(RequestType.READ);
    }

    @Override
    public boolean isAttributeWriteAllowed(ObjectName objectName, String attribute) {
        return isAllowedDomain(objectName) && isTypeAllowed(RequestType.WRITE);
    }

    @Override
    public boolean isOperationAllowed(ObjectName objectName, String operation) {
        return isAllowedDomain(objectName) && isTypeAllowed(RequestType.EXEC);
    }

    @Override
    public boolean isObjectNameHidden(ObjectName objectName) {
        return !isAllowedDomain(objectName);
    }

    private boolean isAllowedDomain(ObjectName objectName) {
        return ALLOWED_DOMAINS.contains(objectName.getDomain());
    }

    private static boolean isLoopbackAddress(String hostOrAddress) {
        return JolokiaHostUtils.isLoopbackAddress(hostOrAddress);
    }

    private static PolicyRestrictor loadPolicyRestrictor() {
        String location = ConfigProvider.getConfig()
                .getOptionalValue("quarkus.camel.jolokia.additional-properties.policyLocation", String.class)
                .orElse(DEFAULT_POLICY_LOCATION);
        try {
            return RestrictorFactory.lookupPolicyRestrictor(location);
        } catch (Exception e) {
            LOG.warnf("Failed to load Jolokia policy file from %s, policy delegation disabled", location, e);
            return null;
        }
    }
}
