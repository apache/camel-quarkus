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

import java.util.Set;

import javax.management.ObjectName;

import io.smallrye.config.SmallRyeConfig;
import org.apache.camel.quarkus.jolokia.config.JolokiaBuildTimeConfig;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jolokia.server.core.restrictor.AllowAllRestrictor;

public final class CamelJolokiaRestrictor extends AllowAllRestrictor {
    private static final Set<String> ALLOWED_DOMAINS = ConfigProvider.getConfig()
            .unwrap(SmallRyeConfig.class)
            .getConfigMapping(JolokiaBuildTimeConfig.class)
            .camelRestrictorAllowedMbeanDomains();

    @Override
    public boolean isAttributeReadAllowed(ObjectName objectName, String attribute) {
        return isAllowedDomain(objectName);
    }

    @Override
    public boolean isAttributeWriteAllowed(ObjectName objectName, String attribute) {
        return isAllowedDomain(objectName);
    }

    @Override
    public boolean isOperationAllowed(ObjectName objectName, String operation) {
        return isAllowedDomain(objectName);
    }

    @Override
    public boolean isObjectNameHidden(ObjectName objectName) {
        return !isAllowedDomain(objectName);
    }

    private boolean isAllowedDomain(ObjectName objectName) {
        return ALLOWED_DOMAINS.contains(objectName.getDomain());
    }
}
