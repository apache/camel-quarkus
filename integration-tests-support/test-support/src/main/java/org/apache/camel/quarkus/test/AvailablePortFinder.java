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
package org.apache.camel.quarkus.test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Finds currently available server ports.
 */
public final class AvailablePortFinder {
    private static final Logger LOGGER = LoggerFactory.getLogger(AvailablePortFinder.class);
    private static final String[] QUARKUS_PORT_PROPERTIES = new String[] {
            "quarkus.http.test-port",
            "quarkus.http.test-ssl-port",
    };

    /**
     * Creates a new instance.
     */
    private AvailablePortFinder() {
        // Do nothing
    }

    /**
     * Gets the next available port.
     *
     * @throws IllegalStateException if there are no ports available
     * @return                       the available port
     */
    public static int getNextAvailable() {
        while (true) {
            try (ServerSocket ss = new ServerSocket()) {
                ss.setReuseAddress(true);
                ss.bind(new InetSocketAddress((InetAddress) null, 0), 1);

                int port = ss.getLocalPort();
                if (!isQuarkusReservedPort(port)) {
                    LOGGER.info("getNextAvailable() -> {}", port);
                    return port;
                }
            } catch (IOException e) {
                throw new IllegalStateException("Cannot find free port", e);
            }
        }
    }

    /**
     * Reserve a list of random and not in use network ports and place them in Map.
     */
    public static Map<String, Integer> reserveNetworkPorts(String... names) {
        return reserveNetworkPorts(Function.identity(), names);
    }

    /**
     * Reserve a list of random and not in use network ports and place them in Map.
     */
    public static <T> Map<String, T> reserveNetworkPorts(Function<Integer, T> converter, String... names) {
        Map<String, T> reservedPorts = new HashMap<>();

        for (String name : names) {
            reservedPorts.put(name, converter.apply(getNextAvailable()));
        }

        return reservedPorts;
    }

    private static boolean isQuarkusReservedPort(int port) {
        Config config = ConfigProvider.getConfig();
        for (String property : QUARKUS_PORT_PROPERTIES) {
            Optional<Integer> portProperty = config.getOptionalValue(property, Integer.class);
            if (portProperty.isPresent()) {
                if (port == portProperty.get()) {
                    LOGGER.info("Port {} is already reserved for {}", port, property);
                    return true;
                }
            }
        }
        return false;
    }
}
