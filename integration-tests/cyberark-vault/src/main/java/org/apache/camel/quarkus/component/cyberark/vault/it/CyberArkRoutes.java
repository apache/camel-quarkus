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
package org.apache.camel.quarkus.component.cyberark.vault.it;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class CyberArkRoutes extends RouteBuilder {

    @ConfigProperty(name = "conjur.url")
    String url;
    @ConfigProperty(name = "conjur.account")
    String account;
    @ConfigProperty(name = "conjur.write.username")
    String writeUsername;
    @ConfigProperty(name = "conjur.write.apiKey")
    String writeApiKey;
    @ConfigProperty(name = "conjur.read.username")
    String readUsername;
    @ConfigProperty(name = "conjur.read.apiKey")
    String readApiKey;

    @Override
    public void configure() throws Exception {

        from("direct:createSecret")
                .toF("cyberark-vault:secret?operation=createSecret&secretId=BotApp/secretVar&url=%s&account=%s&username=%s&apiKey=%s",
                        url, account, writeUsername, writeApiKey)
                .log("Secret created/updated");

        from("direct:createSecretUnauthorized")
                .toF("cyberark-vault:secret?operation=createSecret&secretId=BotApp/secretVar&url=%s&account=%s&username=%s&apiKey=%s",
                        url, account, readUsername, readApiKey)
                .log("Secret created/updated");

        from("direct:getSecret")
                .toF("cyberark-vault:secret?secretId=BotApp/secretVar&url=%s&account=%s&username=%s&apiKey=%s",
                        url, account, readUsername, readApiKey)
                .log("Retrieved secret: ${body}");

    }
}
