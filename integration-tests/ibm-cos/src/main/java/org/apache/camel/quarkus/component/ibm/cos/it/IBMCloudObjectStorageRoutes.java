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
package org.apache.camel.quarkus.component.ibm.cos.it;

import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.endpoint.EndpointRouteBuilder;
import org.apache.camel.builder.endpoint.dsl.IBMCOSEndpointBuilderFactory.IBMCOSEndpointBuilder;
import org.apache.camel.builder.endpoint.dsl.IBMCOSEndpointBuilderFactory.IBMCOSEndpointConsumerBuilder;
import org.apache.camel.builder.endpoint.dsl.IBMCOSEndpointBuilderFactory.IBMCOSEndpointProducerBuilder;
import org.apache.camel.component.ibm.cos.IBMCOSOperations;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class IBMCloudObjectStorageRoutes extends EndpointRouteBuilder {

    public static final String KEY_OF_OBJECT_CREATED = "key-of-object-created";

    public static final String CONSUME_ROUTE_ID = "consumeRoute";

    protected static final String BUCKET_NAME = "camel-test-" + UUID.randomUUID().toString().substring(0, 12).toLowerCase();

    @ConfigProperty(name = "camel.ibm.cos.apiKey")
    String ibmCosApiKey;

    @ConfigProperty(name = "camel.ibm.cos.serviceInstanceId")
    String ibmCosServiceInstanceId;

    @ConfigProperty(name = "camel.ibm.cos.endpointUrl")
    String ibmCosEndpointUrl;

    @ConfigProperty(name = "camel.ibm.cos.location")
    Optional<String> ibmCosLocation;

    @Override
    public void configure() throws Exception {

        from("direct:create-bucket")
                .to(componentUri(IBMCOSOperations.createBucket));

        from("direct:delete-bucket")
                .to(componentUri(IBMCOSOperations.deleteBucket));

        from("direct:put-object")
                .to(componentUri(IBMCOSOperations.putObject).keyName(KEY_OF_OBJECT_CREATED));

        from("direct:delete-object")
                .to(componentUri(IBMCOSOperations.deleteObject).keyName(KEY_OF_OBJECT_CREATED));

        from("direct:read")
                .to(componentUri(IBMCOSOperations.getObject).keyName(KEY_OF_OBJECT_CREATED));

        from("direct:list")
                .to(componentUri(IBMCOSOperations.listObjects));

        from(componentUri())
                .routeId(CONSUME_ROUTE_ID).autoStartup(false)
                .to("mock:result");
    }

    public IBMCOSEndpointConsumerBuilder componentUri() {
        return baseComponentUri()
                .deleteAfterRead(true)
                .maxMessagesPerPoll(10)
                .delay(2000);
    }

    public IBMCOSEndpointBuilder baseComponentUri() {
        return ibmCos(BUCKET_NAME)
                .apiKey(ibmCosApiKey)
                .serviceInstanceId(ibmCosServiceInstanceId)
                .endpointUrl(ibmCosEndpointUrl)
                .location(ibmCosLocation.orElse("us-south"));
    }

    public IBMCOSEndpointProducerBuilder componentUri(IBMCOSOperations operation) {
        return baseComponentUri()
                .operation(operation);
    }

}
