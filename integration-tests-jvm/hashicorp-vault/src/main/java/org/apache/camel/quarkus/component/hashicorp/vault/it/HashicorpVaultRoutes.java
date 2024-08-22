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
import org.apache.camel.component.hashicorp.vault.HashicorpVaultConstants;
import org.apache.camel.spi.PropertiesComponent;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class HashicorpVaultRoutes extends RouteBuilder {
    public static final String TEST_SECRET_NAME = "my-secret";
    public static final String TEST_SECRET_PATH = "camel-quarkus-secret";

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

        from("direct:getSecret")
                .setHeader(HashicorpVaultConstants.SECRET_PATH).constant(TEST_SECRET_PATH)
                .toF("hashicorp-vault:secret?operation=getSecret&scheme=http&host=%s&port=%d&token=%s", host, port, token);

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
                        PropertiesComponent component = exchange.getContext().getPropertiesComponent();
                        component.resolveProperty("hashicorp:secret:" + TEST_SECRET_PATH).ifPresent(value -> {
                            message.setBody(value.replaceAll("[{}]", "").split("=")[1]);
                        });
                    }
                });
    }
}
