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

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spi.PropertiesComponent;

public class AzureKeyVaultRoutes extends RouteBuilder {
    @Override
    public void configure() throws Exception {
        from("direct:createSecret")
                .to(azureKeyVault("createSecret"));

        from("direct:getSecret")
                .to(azureKeyVault("getSecret"));

        from("direct:deleteSecret")
                .to(azureKeyVault("deleteSecret"));

        from("direct:purgeDeletedSecret")
                .to(azureKeyVault("purgeDeletedSecret"));

        from("direct:propertyPlaceholder")
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        Message message = exchange.getMessage();
                        PropertiesComponent component = exchange.getContext().getPropertiesComponent();
                        component.resolveProperty("azure:camel-quarkus-secret").ifPresent(message::setBody);
                    }
                });
    }

    private String azureKeyVault(String operation) {
        return "azure-key-vault://{{camel.vault.azure.vaultName}}" +
                "?clientId=RAW({{camel.vault.azure.clientId}})" +
                "&clientSecret=RAW({{camel.vault.azure.clientSecret}})" +
                "&tenantId=RAW({{camel.vault.azure.tenantId}})" +
                "&operation=" + operation;
    }
}
