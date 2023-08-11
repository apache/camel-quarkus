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

import java.util.Map;
import java.util.Objects;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.quarkus.test.AvailablePortFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrpcServerTestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcServerTestResource.class);

    @Override
    public Map<String, String> start() {
        try {
            return AvailablePortFinder.reserveNetworkPorts(
                    Objects::toString,
                    "camel.grpc.test.async.server.port",
                    "camel.grpc.test.sync.server.port",
                    "camel.grpc.test.server.exception.port",
                    "camel.grpc.test.forward.completed.server.port",
                    "camel.grpc.test.forward.error.server.port",
                    "camel.grpc.test.route.controlled.server.port",
                    "camel.grpc.test.tls.server.port",
                    "camel.grpc.test.jwt.server.port",
                    "camel.grpc.test.sync.aggregation.server.port",
                    "camel.grpc.test.async.aggregation.server.port");
        } catch (Exception e) {
            throw new RuntimeException("Could not start gRPC server", e);
        }
    }

    @Override
    public void stop() {
        AvailablePortFinder.releaseReservedPorts();
    }
}
