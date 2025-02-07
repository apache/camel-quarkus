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
package org.apache.camel.quarkus.component.azure.key.vault.it;

import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spi.PropertiesComponent;

public class AzureKeyVaultRoutes extends RouteBuilder {
    @Override
    public void configure() throws Exception {
        from("direct:createSecret")
                .autoStartup(false)
                .id("createSecret")
                .to(azureKeyVault("createSecret", false));

        from("direct:getSecret")
                .autoStartup(false)
                .id("getSecret")
                .to(azureKeyVault("getSecret", false));

        from("direct:deleteSecret")
                .autoStartup(false)
                .id("deleteSecret")
                .to(azureKeyVault("deleteSecret", false));

        from("direct:purgeDeletedSecret")
                .autoStartup(false)
                .id("purgeDeletedSecret")
                .to(azureKeyVault("purgeDeletedSecret", false));

        from("direct:createSecretIdentity")
                .to(azureKeyVault("createSecret", true));

        from("direct:getSecretIdentity")
                .to(azureKeyVault("getSecret", true));

        from("direct:deleteSecretIdentity")
                .to(azureKeyVault("deleteSecret", true));

        from("direct:purgeDeletedSecretIdentity")
                .to(azureKeyVault("purgeDeletedSecret", true));

        from("direct:propertyPlaceholder")
                .process(exchange -> {
                    Message message = exchange.getMessage();
                    PropertiesComponent component = exchange.getContext().getPropertiesComponent();
                    component.resolveProperty("azure:camel-quarkus-secret").ifPresent(message::setBody);
                });
    }

    private String azureKeyVault(String operation, boolean useIdentity) {
        StringBuilder sb = new StringBuilder("azure-key-vault://{{camel.vault.azure.vaultName}}" +
                "?operation=" + operation);

        if (useIdentity) {
            sb.append("&credentialType=AZURE_IDENTITY");
        } else {
            //can not use i.e. RAW({{camel.vault.azure.clientSecret}}) as the value is not set in identity profiles
            sb.append("&clientId=").append(System.getenv("AZURE_CLIENT_ID"))
                    .append("&clientSecret=").append(System.getenv("AZURE_CLIENT_SECRET"))
                    .append("&tenantId=").append(System.getenv("AZURE_TENANT_ID"));
        }
        return sb.toString();
    }
}
