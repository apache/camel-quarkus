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
package org.apache.camel.quarkus.component.hbase.it;

import java.net.InetAddress;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.jboss.logging.Logger;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class HBaseTestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOG = Logger.getLogger(HBaseTestResource.class);
    // must be the same as in the config of camel component
    static final Integer CLIENT_PORT = 2181;

    private GenericContainer<?> container;

    @Override
    public Map<String, String> start() {

        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            Consumer<CreateContainerCmd> cmd = e -> {
                e
                        .withPortBindings(new PortBinding(Ports.Binding.bindPort(2181),
                                new ExposedPort(2181)),
                                new PortBinding(Ports.Binding.bindPort(16000),
                                        new ExposedPort(16000)),
                                new PortBinding(Ports.Binding.bindPort(16020),
                                        new ExposedPort(16020)));
                e.withHostName(hostname);
            };

            container = new GenericContainer<>("dajobe/hbase:latest")
                    .withExposedPorts(2181, 16000, 16020)
                    .withCreateContainerCmdModifier(cmd)
                    .withLogConsumer(frame -> System.out.print(frame.getUtf8String()))
                    .waitingFor(
                            Wait.forLogMessage(".*Finished refreshing block distribution cache for 2 regions\\n", 1));
            container.start();

            return Collections.emptyMap();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        try {
            if (container != null) {
                container.stop();
            }
        } catch (Exception e) {
            // Ignored
        }
    }

}
