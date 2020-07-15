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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * Native images built using a docker container end up with {@code javax.net.ssl.trustStore} system property
 * pointing to a non-existing file; see https://quarkus.io/guides/native-and-ssl For that case, we have to set
 * {@code javax.net.ssl.trustStore} to an existing path explicitly.
 */
public class TrustStoreResource implements QuarkusTestResourceLifecycleManager {
    private static final String CACERTS_REL_PATH = "lib/security/cacerts";
    private static final String CACERTS_REL_PATH_ALT = "jre/lib/security/cacerts";

    @Override
    public Map<String, String> start() {
        final String graalVmHome = System.getenv("GRAALVM_HOME");
        final String javaHome = System.getProperty("java.home", System.getenv("JAVA_HOME"));

        Path trustStorePath;

        if (graalVmHome != null && !graalVmHome.isEmpty()
                && Files.exists(trustStorePath = Paths.get(graalVmHome).resolve(CACERTS_REL_PATH))) {
            // empty body
        } else if (javaHome != null && !javaHome.isEmpty()
                && Files.exists(trustStorePath = Paths.get(javaHome).resolve(CACERTS_REL_PATH))) {
            // empty body
        } else if (javaHome != null && !javaHome.isEmpty()
                && Files.exists(trustStorePath = Paths.get(javaHome).resolve(CACERTS_REL_PATH_ALT))) {
            // empty body
        } else {
            throw new IllegalStateException(
                    "Could not find any existing file to set javax.net.ssl.trustStore; tried $GRAALVM_HOME/" + CACERTS_REL_PATH
                            + " and $JAVA_HOME/" + CACERTS_REL_PATH
                            + ". You may need to set GRAALVM_HOME or JAVA_HOME properly. Found $GRAALVM_HOME = " + graalVmHome
                            + " and $JAVA_HOME = " + graalVmHome);
        }

        return Collections.singletonMap("javax.net.ssl.trustStore", trustStorePath.toString());
    }

    @Override
    public void stop() {
    }

}
