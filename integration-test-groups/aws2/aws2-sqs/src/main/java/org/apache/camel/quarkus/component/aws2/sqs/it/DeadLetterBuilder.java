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
package org.apache.camel.quarkus.component.aws2.sqs.it;

import javax.enterprise.context.ApplicationScoped;

import org.apache.camel.builder.RouteBuilder;
import org.eclipse.microprofile.config.ConfigProvider;

@ApplicationScoped
public class DeadLetterBuilder extends RouteBuilder {

    @Override
    public void configure() {
        String name = ConfigProvider.getConfig().getValue("aws-sqs.failing-name", String.class);
        String deadLetterName = ConfigProvider.getConfig().getValue("aws-sqs.deadletter-name", String.class);
        errorHandler(deadLetterChannel("aws2-sqs://" + deadLetterName)
                .log("Error processing message and sending to the Dead Letter Queue: Body: " + body())
                .useOriginalMessage());

        from("aws2-sqs://" + name)
                .process(e -> {
                    throw new IllegalArgumentException();
                });

    }
}
