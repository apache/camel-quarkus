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

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class MailMicrosoftOauthRoute extends RouteBuilder {

    @ConfigProperty(name = MailMicrosoftOauthResource.TEST_SUBJECT_PROPERTY)
    String testSubject;

    @Inject
    CamelContext camelContext;

    @Override
    public void configure() {
        fromF("imaps://outlook.office365.com:993"
                + "?authenticator=#auth"
                + "&mail.imaps.auth.mechanisms=XOAUTH2"
                + "&debugMode=true"
                + "&delete=true"
                //search pattern works on contains and not  start with
                + "&searchTerm.subject=" + testSubject.substring(1))
                .id("receiverRoute")
                .autoStartup(false)
                .to("mock:receivedMessages");
    }

    static class Producers {

        @Singleton
        @Produces
        @Named("mailReceivedMessages")
        List<Map<String, Object>> mailReceivedMessages() {
            return new CopyOnWriteArrayList<>();
        }
    }
}
