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
package org.apache.camel.quarkus.component.azure.servicebus.it;

import java.util.Arrays;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class AzureServiceBusRoutes extends RouteBuilder {

    @ConfigProperty(name = "azure.servicebus.connection-string")
    String azureServiceBusConnectionString;

    @Override
    public void configure() {
        from("direct:producer-test")
                .process(exchange -> {
                    final List<String> inputBatch = Arrays.asList("Bulbasaur", "Pikachu", "Charizard", "Squirtle");
                    exchange.getIn().setBody(inputBatch);
                })
                .to("azure-servicebus:test?connectionString=RAW(" + azureServiceBusConnectionString + ")");

        from("azure-servicebus:test?connectionString=RAW(" + azureServiceBusConnectionString + ")")
                .log("${body}")
                .to("mock:azure-servicebus-consumed");
    }
}
