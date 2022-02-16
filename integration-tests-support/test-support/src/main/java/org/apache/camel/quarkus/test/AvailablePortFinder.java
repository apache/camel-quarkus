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
import java.util.concurrent.ConcurrentHashMap;
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
    private static final Map<Integer, String> RESERVED_PORTS = new ConcurrentHashMap<>();
    private static final String[] QUARKUS_PORT_PROPERTIES = new String[] {
            "quarkus.http.test-port",
            "quarkus.http.test-ssl-port",
            "quarkus.https.test-port",
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
        // Using AvailablePortFinder in native applications can be problematic
        // E.g The reserved port may be allocated at build time and preserved indefinitely at runtime. I.e it never changes on each execution of the native application
        logWarningIfNativeApplication();

        while (true) {
            try (ServerSocket ss = new ServerSocket()) {
                ss.setReuseAddress(true);
                ss.bind(new InetSocketAddress((InetAddress) null, 0), 1);

                int port = ss.getLocalPort();
                if (!isQuarkusReservedPort(port)) {
                    String callerClassName = getCallerClassName();
                    String value = RESERVED_PORTS.putIfAbsent(port, callerClassName);
                    if (value == null) {
                        LOGGER.info("{} reserved port {}", callerClassName, port);
                        return port;
                    }
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

    public static void releaseReservedPorts() {
        String callerClassName = getCallerClassName();
        RESERVED_PORTS.entrySet()
                .stream()
                .filter(entry -> entry.getValue().equals(callerClassName))
                .peek(entry -> LOGGER.info("Releasing port {} reserved by {}", entry.getKey(), entry.getValue()))
                .map(Map.Entry::getKey)
                .forEach(RESERVED_PORTS::remove);
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

    private static String getCallerClassName() {
        return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .walk(s -> s.map(StackWalker.StackFrame::getClassName)
                        .filter(className -> !className.equals(AvailablePortFinder.class.getName()))
                        .findFirst()
                        .orElseThrow(IllegalStateException::new));
    }

    private static void logWarningIfNativeApplication() {
        if (System.getProperty("org.graalvm.nativeimage.kind") != null) {
            LOGGER.warn("Usage of AvailablePortFinder in the native application is discouraged. "
                    + "Pass the reserved port to the native application under test with QuarkusTestResource or via an HTTP request");
        }
    }
}
