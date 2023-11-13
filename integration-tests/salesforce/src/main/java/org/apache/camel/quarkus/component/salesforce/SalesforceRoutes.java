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
package org.apache.camel.quarkus.component.salesforce;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.salesforce.AuthenticationType;
import org.apache.camel.component.salesforce.SalesforceComponent;
import org.apache.camel.quarkus.component.salesforce.generated.Account;
import org.apache.camel.quarkus.component.salesforce.model.TestEventPojo;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class SalesforceRoutes extends RouteBuilder {

    @ConfigProperty(name = "SALESFORCE_USERNAME", defaultValue = "username")
    String username;

    @ConfigProperty(name = "SALESFORCE_PASSWORD", defaultValue = "password")
    String password;

    @ConfigProperty(name = "SALESFORCE_CLIENTID", defaultValue = "clientId")
    String clientId;

    @ConfigProperty(name = "SALESFORCE_CLIENTSECRET", defaultValue = "clientSecret")
    String clientSecret;

    @Named("salesforce")
    SalesforceComponent salesforceComponent() {
        // check if wiremock URL exists
        Optional<String> wireMockUrl = ConfigProvider.getConfig().getOptionalValue("wiremock.url", String.class);

        SalesforceComponent salesforceComponent = new SalesforceComponent();
        salesforceComponent.setClientId(clientId);
        salesforceComponent.setClientSecret(clientSecret);
        salesforceComponent.setUserName(username);
        salesforceComponent.setPassword(password);
        salesforceComponent.setPackages("org.apache.camel.quarkus.component.salesforce.generated");

        // Set URL depending if mock is enabled
        if (wireMockUrl.isPresent()) {
            salesforceComponent.setAuthenticationType(AuthenticationType.USERNAME_PASSWORD);
            salesforceComponent.setRefreshToken("refreshToken");
            salesforceComponent.setLoginUrl(wireMockUrl.get());
        } else {
            salesforceComponent.setLoginUrl("https://login.salesforce.com");
        }
        return salesforceComponent;
    }

    @Override
    public void configure() throws Exception {
        Optional<String> wireMockUrl = ConfigProvider.getConfig().getOptionalValue("wiremock.url", String.class);
        // Wiremock used only with Templates - this Route is used only with Salesforce credentials
        if (!wireMockUrl.isPresent()) {
            // Change Data Capture
            from("salesforce:subscribe:/data/AccountChangeEvent?replayId=-1")
                    .routeId("cdc")
                    .autoStartup(false)
                    .to("seda:events");

            // Streaming API : topic consumer - getting Account object
            from("salesforce:subscribe:CamelTestTopic?notifyForFields=ALL&"
                    + "notifyForOperationCreate=true&notifyForOperationDelete=true&notifyForOperationUpdate=true&"
                    + "sObjectClass=" + Account.class.getName() + "&updateTopic=true&sObjectQuery=SELECT Id, Name FROM Account")
                    .to("seda:CamelTestTopic");

            // Streaming API : topic consumer with RAW Payload - getting json as String
            from("salesforce:subscribe:CamelTestTopic?rawPayload=true&notifyForFields=ALL&"
                    + "notifyForOperationCreate=true&notifyForOperationDelete=true&notifyForOperationUpdate=true&"
                    + "updateTopic=true&sObjectQuery=SELECT Id, Name FROM Account")
                    .to("seda:RawPayloadCamelTestTopic");

            // it takes some time for the subscriber to subscribe, so we'll try to
            // send repeated platform events and wait until the first one is
            // received
            from("timer:platform")
                    .autoStartup(false)
                    .routeId("platformEventTimer")
                    .setBody().simple("{\"Test_Field__c\": \"data\"}")
                    .to("salesforce:createSObject?sObjectName=TestEvent__e");

            // Pub / sub API
            from("direct:pubSubPublish")
                    .log("Sent Salesforce topic message: ${body}")
                    .to("salesforce:pubSubPublish:/event/TestEvent__e");

            // AVRO deserialize type (also analogous to SPECIFIC_RECORD)
            from("salesforce:pubSubSubscribe:/event/TestEvent__e")
                    .routeId("topicSubscribeAvro")
                    .autoStartup(false)
                    .log("Received Salesforce AVRO topic message: ${body}")
                    .to("seda:pubSubSubscribeAvro");

            // GENERIC_RECORD deserialize type
            from("salesforce:pubSubSubscribe:/event/TestEvent__e?pubSubDeserializeType=GENERIC_RECORD")
                    .routeId("topicSubscribeGenericRecord")
                    .autoStartup(false)
                    .log("Received Salesforce GENERIC_RECORD topic message: ${body}")
                    .to("seda:pubSubSubscribeGenericRecord");

            // JSON deserialize type
            from("salesforce:pubSubSubscribe:/event/TestEvent__e?pubSubDeserializeType=JSON")
                    .routeId("topicSubscribeJson")
                    .autoStartup(false)
                    .log("Received Salesforce JSON topic message: ${body}")
                    .to("seda:pubSubSubscribeJson");

            // POJO deserialize type
            from("salesforce:pubSubSubscribe:/event/TestEvent__e?pubSubDeserializeType=POJO&pubSubPojoClass="
                    + TestEventPojo.class.getName())
                    .routeId("topicSubscribePojo")
                    .autoStartup(false)
                    .log("Received Salesforce POJO topic message: ${body}")
                    .to("seda:pubSubSubscribePojo");
        }
    }
}
