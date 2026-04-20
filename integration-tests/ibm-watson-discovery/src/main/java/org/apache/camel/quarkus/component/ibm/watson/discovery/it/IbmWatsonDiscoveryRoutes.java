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
package org.apache.camel.quarkus.component.ibm.watson.discovery.it;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.ibm.watson.discovery.WatsonDiscoveryConstants;
import org.apache.camel.component.ibm.watson.discovery.WatsonDiscoveryOperations;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class IbmWatsonDiscoveryRoutes extends RouteBuilder {

    @ConfigProperty(name = "camel.ibm.watson.serviceUrl")
    Optional<String> serviceUrl;

    @ConfigProperty(name = "camel.ibm.watson.apiKey")
    Optional<String> apiKey;

    @ConfigProperty(name = "camel.ibm.watson.projectId")
    Optional<String> projectId;

    @Override
    public void configure() {
        if (serviceUrl.isPresent() && apiKey.isPresent() && projectId.isPresent()) {
            String baseUri = String.format("ibm-watson-discovery:test?serviceUrl=%s&apiKey=%s&projectId=%s",
                    serviceUrl.get(), apiKey.get(), projectId.get());

            // Collection operations
            from("direct:list-collections")
                    .setHeader(WatsonDiscoveryConstants.OPERATION, constant(WatsonDiscoveryOperations.listCollections))
                    .to(baseUri);

            from("direct:create-collection")
                    .setHeader(WatsonDiscoveryConstants.OPERATION, constant(WatsonDiscoveryOperations.createCollection))
                    .to(baseUri);

            from("direct:delete-collection")
                    .setHeader(WatsonDiscoveryConstants.OPERATION, constant(WatsonDiscoveryOperations.deleteCollection))
                    .to(baseUri);

            // Document operations
            from("direct:add-document")
                    .setHeader(WatsonDiscoveryConstants.OPERATION, constant(WatsonDiscoveryOperations.addDocument))
                    .to(baseUri);

            from("direct:update-document")
                    .setHeader(WatsonDiscoveryConstants.OPERATION, constant(WatsonDiscoveryOperations.updateDocument))
                    .to(baseUri);

            from("direct:delete-document")
                    .setHeader(WatsonDiscoveryConstants.OPERATION, constant(WatsonDiscoveryOperations.deleteDocument))
                    .to(baseUri);

            // Query operation
            from("direct:query")
                    .setHeader(WatsonDiscoveryConstants.OPERATION, constant(WatsonDiscoveryOperations.query))
                    .to(baseUri);
        }
    }
}
