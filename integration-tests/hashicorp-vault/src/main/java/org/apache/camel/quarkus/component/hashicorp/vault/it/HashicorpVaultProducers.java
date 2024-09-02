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
package org.apache.camel.quarkus.component.hashicorp.vault.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultTemplate;

@ApplicationScoped
public class HashicorpVaultProducers {
    @ConfigProperty(name = "camel.vault.hashicorp.scheme")
    String scheme;

    @ConfigProperty(name = "camel.vault.hashicorp.host")
    String host;

    @ConfigProperty(name = "camel.vault.hashicorp.port")
    int port;

    @ConfigProperty(name = "camel.vault.hashicorp.token")
    String token;

    @Named("customVaultTemplate")
    VaultTemplate customVaultTemplate() {
        VaultEndpoint endpoint = new VaultEndpoint();
        endpoint.setScheme(scheme);
        endpoint.setHost(host);
        endpoint.setPort(port);
        return new VaultTemplate(endpoint, new TokenAuthentication(token));
    }
}
