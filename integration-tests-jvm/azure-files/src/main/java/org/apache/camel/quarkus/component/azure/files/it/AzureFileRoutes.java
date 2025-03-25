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
package org.apache.camel.quarkus.component.azure.files.it;

import org.apache.camel.builder.RouteBuilder;

public class AzureFileRoutes extends RouteBuilder {
    private static final String AZURE_FILES_URI = "azure-files://{{azure.storage.account-name}}/{{azure.files.share.name}}/{{azure.files.share.directory.name}}?credentialType=SHARED_ACCOUNT_KEY&sharedKey=RAW({{azure.storage.account-key}})";

    @Override
    public void configure() throws Exception {
        from(AZURE_FILES_URI + "&delete=true")
                .routeId("azure-files-consumer")
                .autoStartup(false)
                .to("seda:downloadedFiles");

        from("direct:uploadFile")
                .to(AZURE_FILES_URI);
    }
}
