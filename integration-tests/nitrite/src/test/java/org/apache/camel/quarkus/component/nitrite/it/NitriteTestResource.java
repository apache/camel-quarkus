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

package org.apache.camel.quarkus.component.nitrite.it;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.util.CollectionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NitriteTestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(NitriteTestResource.class);

    private Path dbFile;

    @Override
    public Map<String, String> start() {

        try {
            String filePrefix = getClass().getSimpleName() + "-db-file-";
            LOGGER.debug("Creating temporary file for Nitrite db ({}*)", filePrefix);
            dbFile = Files.createTempFile(filePrefix, "");

            return CollectionHelper.mapOf(NitriteResource.PROPERTY_DB_FILE, dbFile.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        try {
            if (dbFile != null) {
                Files.deleteIfExists(dbFile);
            }
        } catch (Exception e) {
            // ignored
        }
    }
}
