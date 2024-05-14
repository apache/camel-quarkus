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

import java.security.Provider;
import java.security.Security;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class FipsModeUtil {

    private FipsModeUtil() {
    }

    /**
     * Returns null if system is not in fips mode.
     * Returns Optional.empty if system is in fips mode and there is some provider containing "fips"
     * Returns Optional.name if system is in fips mode and there is a match with the provided providers
     * (the last 2 options allows differentiate reason of the enablement/disablement)
     */
    static public Optional<String> findFipsProvider(List<String> providersToMatch) {
        Provider[] jdkProviders = Security.getProviders();
        int matchCount = 0;

        for (Provider provider : jdkProviders) {
            if (providersToMatch.isEmpty() && provider.getName().toLowerCase().contains("fips")) {
                return Optional.of(provider.getName());
            } else if (providersToMatch.contains(provider.getName())) {
                matchCount++;
            }
        }

        if (!providersToMatch.isEmpty() && matchCount == providersToMatch.size()) {
            return Optional.empty();
        }

        return null;
    }

    /**
     * Return true if system is in FIPS mode.
     */
    static public boolean isFipsMode() {
        return findFipsProvider(Collections.emptyList()) != null;
    }
}
