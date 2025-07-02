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
package org.apache.camel.quarkus.component.mail.microsoft.oauth.it;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import org.apache.camel.component.mail.microsoft.authenticator.MicrosoftExchangeOnlineOAuth2MailAuthenticator;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class MailMicrosoftOauthProducers {

    private static final Logger LOGGER = LoggerFactory.getLogger(MailMicrosoftOauthProducers.class);

    @ConfigProperty(name = MailMicrosoftOauthResource.USERNAME_PROPERTY)
    Optional<String> username;

    @ConfigProperty(name = MailMicrosoftOauthResource.TENANT_ID_PROPERTY)
    Optional<String> tenantId;

    @ConfigProperty(name = MailMicrosoftOauthResource.CLIENT_ID_PROPERTY)
    Optional<String> clientId;

    @ConfigProperty(name = MailMicrosoftOauthResource.CLIENT_SECRET_PROPERTY)
    Optional<String> clientSecret;

    @jakarta.enterprise.inject.Produces
    @Named("auth")
    public MicrosoftExchangeOnlineOAuth2MailAuthenticator exchangeAuthenticator() {
        if (username.isPresent() && tenantId.isPresent() && clientId.isPresent() && clientSecret.isPresent()) {
            return new MicrosoftExchangeOnlineOAuth2MailAuthenticator(tenantId.get(), clientId.get(), clientSecret.get(),
                    username.get());
        }
        LOGGER.debug(
                "Set CQ_MAIL_MICROSOFT_OAUTH_USERNAME, CQ_MAIL_MICROSOFT_OAUTH_CLIENT_ID, CQ_MAIL_MICROSOFT_OAUTH_CLIENT_SECRET and CQ_MAIL_MICROSOFT_OAUTH_TENANT_ID env vars.");
        return null;
    }
}
