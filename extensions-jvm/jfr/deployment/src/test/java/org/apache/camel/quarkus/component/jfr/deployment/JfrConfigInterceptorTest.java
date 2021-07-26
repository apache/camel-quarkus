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
package org.apache.camel.quarkus.component.jfr.deployment;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Properties;

import io.quarkus.test.QuarkusUnitTest;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class JfrConfigInterceptorTest {

    @TempDir
    static File recordingsDir;

    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(applicationProperties(), "application.properties"));

    //@Test
    public void testCamelMainPropertiesMappedToJfrProperties() {
        assertEquals("true", getConfigValue("quarkus.camel.jfr.startupRecorderRecording"));
        assertEquals("30", getConfigValue("quarkus.camel.jfr.startup-recorder-duration"));
        assertEquals("10", getConfigValue("quarkus.camel.jfr.startup-recorder-max-depth"));
        assertEquals("profile", getConfigValue("quarkus.camel.jfr.startup-recorder-profile"));

        String recordDir = getConfigValue("quarkus.camel.jfr.startup-recorder-dir");
        String home = System.getProperty("user.home");
        assertFalse(recordDir.startsWith(home));
    }

    public static final Asset applicationProperties() {
        Writer writer = new StringWriter();

        Properties props = new Properties();
        props.setProperty("camel.main.startup-recorder-dir", recordingsDir.getAbsolutePath());
        props.setProperty("camel.main.startupRecorderRecording", "true");
        props.setProperty("camel.main.startup-recorder-duration", "30");
        props.setProperty("camel.main.startup-recorder-max-depth", "10");
        props.setProperty("camel.main.startup-recorder-profile", "profile");

        try {
            props.store(writer, "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new StringAsset(writer.toString());
    }

    private String getConfigValue(String key) {
        return ConfigProvider.getConfig().getValue(key, String.class);
    }
}
