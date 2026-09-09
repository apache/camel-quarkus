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

import java.net.InetAddress;
import java.net.UnknownHostException;

public final class JolokiaHostUtils {

    private JolokiaHostUtils() {
        // Utility class
    }

    /**
     * Determines whether the given host name or address identifies the loopback interface.
     *
     * Host names are never resolved, since this is used to classify client supplied values and a name that
     * resolves to a loopback address must not be treated as local. Only the well known loopback names are
     * recognized.
     */
    public static boolean isLoopbackAddress(String hostOrAddress) {
        if (hostOrAddress == null || hostOrAddress.isEmpty()) {
            return false;
        }

        String host = hostOrAddress;
        if (host.startsWith("[") && host.endsWith("]")) {
            host = host.substring(1, host.length() - 1);
        }

        // A trailing dot is the root form of the same name
        String name = host.endsWith(".") ? host.substring(0, host.length() - 1) : host;
        if ("localhost".equalsIgnoreCase(name) || "localhost.localdomain".equalsIgnoreCase(name)) {
            return true;
        }

        if (host.indexOf(':') != -1) {
            // The scope id names a local interface, so dropping it keeps the verdict independent of the
            // interfaces this host has
            int scope = host.indexOf('%');
            if (scope >= 0) {
                if (!isScopeId(host.substring(scope + 1))) {
                    return false;
                }
                host = host.substring(0, scope);
            }
            if (!isIPv6LiteralCharacterSet(host)) {
                return false;
            }
            // InetAddress resolves anything it cannot read as a literal, and only reads one beginning with a
            // hex digit or a colon. Brackets and this guard keep a client supplied value away from the resolver
            char first = host.charAt(0);
            if (first != ':' && !isHexDigit(first)) {
                return false;
            }
            try {
                return InetAddress.getByName("[" + host + "]").isLoopbackAddress();
            } catch (UnknownHostException e) {
                return false;
            }
        }

        return isIPv4Loopback(host);
    }

    private static boolean isScopeId(String scopeId) {
        if (scopeId.isEmpty()) {
            return false;
        }
        for (int i = 0; i < scopeId.length(); i++) {
            char c = scopeId.charAt(i);
            boolean allowed = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || c == '.' || c == '-' || c == '_';
            if (!allowed) {
                return false;
            }
        }
        return true;
    }

    private static boolean isIPv6LiteralCharacterSet(String address) {
        if (address.isEmpty()) {
            return false;
        }
        for (int i = 0; i < address.length(); i++) {
            char c = address.charAt(i);
            if (!isHexDigit(c) && c != ':' && c != '.') {
                return false;
            }
        }
        return true;
    }

    private static boolean isHexDigit(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private static boolean isIPv4Loopback(String ip) {
        String[] parts = ip.split("\\.", -1);
        if (parts.length != 4) {
            return false;
        }
        if (parseOctet(parts[0]) != 127) {
            return false;
        }
        for (int i = 1; i < 4; i++) {
            if (parseOctet(parts[i]) < 0) {
                return false;
            }
        }
        return true;
    }

    private static int parseOctet(String value) {
        if (value.isEmpty() || value.length() > 3) {
            return -1;
        }
        int octet = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < '0' || c > '9') {
                return -1;
            }
            octet = octet * 10 + (c - '0');
        }
        return octet <= 255 ? octet : -1;
    }
}
