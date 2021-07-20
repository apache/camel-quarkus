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
package org.apache.camel.quarkus.component.netty.tcp;

import java.util.Map;
import java.util.Objects;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.quarkus.test.AvailablePortFinder;

public class NettyTcpTestResource implements QuarkusTestResourceLifecycleManager {
    @Override
    public Map<String, String> start() {
        return AvailablePortFinder.reserveNetworkPorts(
                Objects::toString,
                "camel.netty.test-tcp-port",
                "camel.netty.test-bytebuf-tcp-port",
                "camel.netty.test-codec-tcp-port",
                "camel.netty.test-ssl-tcp-port",
                "camel.netty.test-server-initializer-tcp-port",
                "camel.netty.test-worker-group-tcp-port",
                "camel.netty.test-correlation-manager-tcp-port",
                "camel.netty.test-serialization-tcp-port");
    }

    @Override
    public void stop() {
    }
}
