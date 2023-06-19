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
package org.apache.camel.quarkus.component.netty.http;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class NettyHttpJaasTestResource extends NettyHttpTestResource {
    private static final String JAAS_FILE_NAME = "config.jaas";
    private static final Path SOURCE_FILE = Paths.get("src/test/resources/").resolve(JAAS_FILE_NAME);
    private static final Path TARGET_FILE = Paths.get("target/").resolve(JAAS_FILE_NAME);

    @Override
    public Map<String, String> start() {
        if (!Files.exists(TARGET_FILE)) {
            try {
                Files.copy(SOURCE_FILE, TARGET_FILE);
            } catch (IOException e) {
                throw new RuntimeException("Unable to copy " + JAAS_FILE_NAME, e);
            }
        }

        // for JVM
        System.setProperty("java.security.auth.login.config", TARGET_FILE.toAbsolutePath().toString());
        final Map<String, String> properties = super.start();
        // for native
        properties.put("java.security.auth.login.config", TARGET_FILE.toAbsolutePath().toString());
        return properties;
    }

    @Override
    public void stop() {
        System.clearProperty("java.security.auth.login.config");
        super.stop();
    }
}
