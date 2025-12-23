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
package org.apache.camel.quarkus.component.oauth.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.oauth.OAuthBearerTokenProcessor;
import org.apache.camel.oauth.OAuthClientCredentialsProcessor;

@ApplicationScoped
public class OathRoutes extends RouteBuilder {

    @Inject
    private CamelContext camelContext;

    @Override
    public void configure() throws Exception {
        from("platform-http:/plain")
                .routeId("plain")
                .setBody(simple("Hello ${header.name} - No auth"));

        from("platform-http:/credentials")
                .routeId("credentials")
                // Obtain an Authorization Token
                .process(new OAuthClientCredentialsProcessor())
                // Extract the Authorization Token
                .process(exc -> {
                    var msg = exc.getMessage();
                    var authToken = msg.getHeader("Authorization", String.class);
                    exc.getIn().setBody(authToken);
                });

        from("platform-http:/bearer")
                .routeId("bearer")
                .process(e -> {
                    camelContext.getGlobalOptions().put("Authorization", e.getIn().getHeader("Authorization", String.class));
                })
                .process(new OAuthBearerTokenProcessor())
                .process(e -> camelContext.getGlobalOptions().remove("Authorization"))
                .setBody(simple("Hello ${header.name} - bearerToken"));

    }
}
