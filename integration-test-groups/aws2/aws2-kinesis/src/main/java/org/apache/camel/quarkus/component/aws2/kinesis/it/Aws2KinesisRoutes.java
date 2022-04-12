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
package org.apache.camel.quarkus.component.aws2.kinesis.it;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.camel.builder.RouteBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class Aws2KinesisRoutes extends RouteBuilder {

    @ConfigProperty(name = "aws-kinesis.stream-name")
    String streamName;

    @Inject
    @Named("aws2KinesisMessages")
    Queue<String> aws2KinesisMessages;

    private String componentUri() {
        return "aws2-kinesis://" + streamName;
    }

    @Override
    public void configure() throws Exception {
        from(componentUri())
                .process(exchange -> aws2KinesisMessages.add(exchange.getMessage().getBody(String.class)));
    }

    static class Producers {
        @Singleton
        @javax.enterprise.inject.Produces
        @Named("aws2KinesisMessages")
        Queue<String> aws2KinesisMessages() {
            return new ConcurrentLinkedDeque<>();
        }
    }

}
