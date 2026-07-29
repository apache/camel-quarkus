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
package org.apache.camel.quarkus.jolokia.util;

public final class JolokiaHostUtils {

    private JolokiaHostUtils() {
        // Utility class
    }

    public static boolean isLoopbackAddress(String hostOrAddress) {
        if (hostOrAddress == null || hostOrAddress.isEmpty()) {
            return false;
        }

        String host = hostOrAddress;
        if (host.startsWith("[") && host.endsWith("]")) {
            host = host.substring(1, host.length() - 1);
        }

        if ("localhost".equalsIgnoreCase(host) || "localhost.localdomain".equalsIgnoreCase(host)) {
            return true;
        }

        if (host.startsWith("127.")) {
            return isIPv4Loopback(host);
        }

        return "::1".equals(host) || "0:0:0:0:0:0:0:1".equals(host);
    }

    private static boolean isIPv4Loopback(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        try {
            if (Integer.parseInt(parts[0]) != 127) {
                return false;
            }
            for (int i = 1; i < 4; i++) {
                int octet = Integer.parseInt(parts[i]);
                if (octet < 0 || octet > 255) {
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
