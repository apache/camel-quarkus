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
package org.apache.camel.quarkus.component.ldap;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "quarkus.camel.ldap")
public interface CamelLdapConfig {

    /**
     * Ldap dirContext configuration
     */
    Map<String, LdapDirContextConfig> dirContexts();

    @ConfigGroup
    interface LdapDirContextConfig {

        /**
         * The initial context factory to use. The value of the property should be the fully qualified class name
         * of the factory class that will create an initial context.
         */
        Optional<String> initialContextFactory();

        /**
         * The service provider
         * to use. The value of the property should contain a URL string
         * (e.g. "ldap://somehost:389").
         */
        Optional<String> providerUrl();

        /**
         * The security protocol to use.
         * Its value is a string determined by the service provider
         * (e.g. "ssl").
         */
        Optional<String> securityProtocol();

        /**
         * The security level to use.
         * Its value is one of the following strings:
         * "none", "simple", "strong".
         * If this property is unspecified,
         * the behaviour is determined by the service provider.
         */
        @WithDefault("none")
        String securityAuthentication();

        /**
         * The custom socket factory to use. The value of the property should be the fully qualified class name
         * of the socket factory class.
         */
        Optional<String> socketFactory();

        /**
         * Any other option which will be used during dirContext creation.
         */
        Map<String, String> additionalOptions();
    }
}
