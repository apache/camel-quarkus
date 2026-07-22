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
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import org.apache.camel.component.cyberark.vault.client.ConjurClient;
import org.apache.camel.component.cyberark.vault.client.ConjurClientFactory;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ConjurClientProducer {

    @ConfigProperty(name = "conjur.url")
    String url;
    @ConfigProperty(name = "conjur.account")
    String account;
    @ConfigProperty(name = "conjur.reader.username")
    String readerUsername;
    @ConfigProperty(name = "conjur.reader.apiKey")
    String readerApiKey;

    @Produces
    @ApplicationScoped
    @Named("myConjurClient")
    ConjurClient createConjurClient() {
        return ConjurClientFactory.createWithApiKey(url, account, readerUsername, readerApiKey);
    }

    void disposeConjurClient(@Disposes @Named("myConjurClient") ConjurClient client) throws Exception {
        client.close();
    }
}
