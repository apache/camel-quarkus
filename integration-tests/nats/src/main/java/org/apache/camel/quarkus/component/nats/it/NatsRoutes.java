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
package org.apache.camel.quarkus.component.nats.it;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.camel.builder.RouteBuilder;

@ApplicationScoped
public class NatsRoutes extends RouteBuilder {

    @Inject
    NatsResource natsResource;

    @Override
    public void configure() {
        from("natsBasicAuth:test").routeId("basic-auth").bean(natsResource, "storeMessage");
        from("natsNoAuth:test").routeId("no-auth").bean(natsResource, "storeMessage");
        from("natsTokenAuth:test").routeId("token-auth").bean(natsResource, "storeMessage");

        from("natsNoAuth:max?maxMessages=2").routeId("2-msg-max").bean(natsResource, "storeMessage");

        String maxMsgUriPattern = "natsNoAuth:qmax?maxMessages=%s&queueName=q";
        fromF(maxMsgUriPattern, 3).routeId("3-qmsg-max").bean(natsResource, "storeMessage");
        fromF(maxMsgUriPattern, 8).routeId("8-qmsg-max").bean(natsResource, "storeMessage");

        from("natsNoAuth:request-reply").setBody().simple("${body} => Reply");
        from("natsNoAuth:reply").routeId("reply").bean(natsResource, "storeMessage");
    }
}
