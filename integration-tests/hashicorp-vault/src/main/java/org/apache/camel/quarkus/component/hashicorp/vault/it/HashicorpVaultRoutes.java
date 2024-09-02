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
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spi.PropertiesComponent;
import org.apache.camel.util.ObjectHelper;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import static org.apache.camel.component.hashicorp.vault.HashicorpVaultConstants.SECRET_PATH;
import static org.apache.camel.component.hashicorp.vault.HashicorpVaultConstants.SECRET_VERSION;

@ApplicationScoped
public class HashicorpVaultRoutes extends RouteBuilder {
    public static final String TEST_SECRET_NAME = "my-secret";
    public static final String TEST_SECRET_PATH = "camel-quarkus-secret";
    public static final String TEST_VERSIONED_SECRET_PATH = "camel-quarkus-secret-versioned";

    @ConfigProperty(name = "camel.vault.hashicorp.host")
    String host;

    @ConfigProperty(name = "camel.vault.hashicorp.port")
    int port;

    @ConfigProperty(name = "camel.vault.hashicorp.token")
    String token;

    @Override
    public void configure() throws Exception {
        from("direct:createSecret")
                .toF("hashicorp-vault:secret?operation=createSecret&scheme=http&host=%s&port=%d&token=%s&secretPath=%s", host,
                        port, token, TEST_SECRET_PATH);

        from("direct:createVersionedSecret")
                .toF("hashicorp-vault:secret?operation=createSecret&scheme=http&host=%s&port=%d&token=%s&secretPath=%s", host,
                        port, token, TEST_VERSIONED_SECRET_PATH);

        from("direct:getSecret")
                .toF("hashicorp-vault:secret?operation=getSecret&scheme=http&host=%s&port=%d&token=%s", host, port, token);

        from("direct:getSecretWithCustomVaultTemplate")
                .to("hashicorp-vault:secret?operation=getSecret&vaultTemplate=#customVaultTemplate");

        from("direct:deleteSecret")
                .toF("hashicorp-vault:secret?operation=deleteSecret&scheme=http&host=%s&port=%d&token=%s&secretPath=%s", host,
                        port, token, TEST_SECRET_PATH);

        from("direct:listSecrets")
                .toF("hashicorp-vault:secret?operation=listSecrets&scheme=http&host=%s&port=%d&token=%s&secretPath=%s", host,
                        port, token, TEST_SECRET_PATH);

        from("direct:propertyPlaceholder")
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        Message message = exchange.getMessage();
                        String path = message.getHeader(SECRET_PATH, String.class);
                        String version = message.getHeader(SECRET_VERSION, String.class);
                        String versionSuffix = "";
                        if (ObjectHelper.isNotEmpty(version)) {
                            versionSuffix = "@" + version;
                        }

                        PropertiesComponent component = exchange.getContext().getPropertiesComponent();
                        component.resolveProperty("hashicorp:secret:" + path + versionSuffix).ifPresent(value -> {
                            message.setBody(value.replaceAll("[{}]", "").split("=")[1]);
                        });
                    }
                });
    }
}
