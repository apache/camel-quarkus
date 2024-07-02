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
package org.apache.camel.quarkus.core.deployment.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.jboss.logging.Logger;

public final class CamelQuarkusVersion {
    private static final String CAMEL_QUARKUS_VERSION_PROPERTIES = "camel-quarkus-version.properties";
    private static final Logger LOG = Logger.getLogger(CamelQuarkusVersion.class);
    private static final String VERSION;

    static {
        Properties versionProps = new Properties();
        String versionString = "unknown version";
        try (final InputStream stream = CamelQuarkusVersion.class.getResourceAsStream(CAMEL_QUARKUS_VERSION_PROPERTIES)) {
            if (stream != null) {
                try (final InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    versionProps.load(reader);
                    versionString = versionProps.getProperty("version", versionString);
                }
            } else {
                logUnableToLoadVersionProperties(null);
            }
        } catch (IOException e) {
            logUnableToLoadVersionProperties(e);
        }
        VERSION = versionString;
    }

    static void logUnableToLoadVersionProperties(IOException e) {
        if (LOG.isDebugEnabled()) {
            LOG.debugf(e, "Unable to load %s", CAMEL_QUARKUS_VERSION_PROPERTIES);
        }
    }

    private CamelQuarkusVersion() {
        // Utility class
    }

    public static String getVersion() {
        return VERSION;
    }
}
