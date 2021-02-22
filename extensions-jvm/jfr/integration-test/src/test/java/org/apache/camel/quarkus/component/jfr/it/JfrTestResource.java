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
package org.apache.camel.quarkus.component.jfr.it;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.util.CollectionHelper;

public class JfrTestResource implements QuarkusTestResourceLifecycleManager {

    public static final Path JFR_RECORDINGS_DIR;

    static {
        try {
            JFR_RECORDINGS_DIR = Files.createTempDirectory("recordings");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, String> start() {
        return CollectionHelper.mapOf(
                "quarkus.camel.jfr.startup-recorder-dir", JFR_RECORDINGS_DIR.toString(),
                "quarkus.camel.jfr.startup-recorder-recording", "true");
    }

    @Override
    public void stop() {
    }
}
