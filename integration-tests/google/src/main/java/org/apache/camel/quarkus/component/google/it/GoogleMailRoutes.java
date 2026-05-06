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
package org.apache.camel.quarkus.component.google.it;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spi.DataType;

@ApplicationScoped
public class GoogleMailRoutes extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        // Route using google-mail:update-message-labels transformer
        from("direct:update-message-labels")
                .transformDataType(new DataType("google-mail:update-message-labels"))
                .to("google-mail://messages/modify?inBody=modifyMessageRequest");

        // Route using google-mail:draft transformer
        from("direct:create-draft")
                .transformDataType(new DataType("google-mail:draft"))
                .to("google-mail://drafts/create?inBody=content");

        // Route using google-mail:draft transformer for update
        from("direct:update-draft")
                .transformDataType(new DataType("google-mail:draft"))
                .to("google-mail://drafts/update?inBody=content");
    }
}
