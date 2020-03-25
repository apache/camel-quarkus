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

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.quarkus.test.AvailablePortFinder;
import org.apache.camel.util.CollectionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrpcServerTestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcServerTestResource.class);
    private Server grpcServer;

    @Override
    public Map<String, String> start() {
        try {
            final int port = AvailablePortFinder.getNextAvailable();
            grpcServer = ServerBuilder.forPort(port).addService(new PingPongImpl()).build().start();
            return CollectionHelper.mapOf(
                    "camel.grpc.test.server.port", String.valueOf(port),
                    "camel.grpc.consumer.port", String.valueOf(AvailablePortFinder.getNextAvailable()));
        } catch (Exception e) {
            throw new RuntimeException("Could not start gRPC server", e);
        }
    }

    @Override
    public void stop() {
        try {
            if (grpcServer != null) {
                grpcServer.shutdown();
            }
        } catch (Exception e) {
            LOGGER.error("Could not stop gRPC server", e);
        }
    }
}
