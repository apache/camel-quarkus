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
package org.apache.camel.quarkus.component.grpc.it;

import java.util.Optional;

import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.quarkus.component.grpc.it.model.PingRequest;
import org.apache.camel.quarkus.component.grpc.it.model.PongResponse;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

public class GrpcRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        fromF("grpc://localhost:%d/org.apache.camel.quarkus.component.grpc.it.model.PingPong?synchronous=true",
                getServerPort())
                        .process(exchange -> {
                            final Message message = exchange.getMessage();
                            final PingRequest request = message.getBody(PingRequest.class);
                            final PongResponse response = PongResponse.newBuilder().setPongName(request.getPingName() + " PONG")
                                    .setPongId(request.getPingId()).build();
                            message.setBody(response);
                        });
    }

    public static int getServerPort() {
        Config config = ConfigProvider.getConfig();
        Optional<Integer> testServerPort = config.getOptionalValue("quarkus.grpc.server.test-port", Integer.class);
        return testServerPort.orElse(9000);
    }
}
