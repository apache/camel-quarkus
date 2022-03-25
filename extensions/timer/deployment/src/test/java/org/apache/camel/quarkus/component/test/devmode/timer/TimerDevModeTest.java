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
package org.apache.camel.quarkus.component.test.devmode.timer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.QuarkusDevModeTest;
import org.awaitility.Awaitility;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class TimerDevModeTest {
    private static final Path LOG_FILE = Paths.get("target/" + TimerDevModeTest.class.getSimpleName() + ".log");

    @RegisterExtension
    static final QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap
                    .create(JavaArchive.class)
                    .addClasses(TimerRoute.class)
                    .addAsResource(applicationProperties(), "application.properties"))
            .setLogFileName(LOG_FILE.getFileName().toString());

    @Test
    void logMessageEdit() throws IOException {
        Awaitility.await()
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> Files.exists(LOG_FILE));

        try (BufferedReader logFileReader = Files.newBufferedReader(LOG_FILE, StandardCharsets.UTF_8)) {
            assertLogMessage(logFileReader, "Hello foo", 30000);
            TEST.modifySourceFile(TimerRoute.class, oldSource -> oldSource.replace("Hello foo", "Hello bar"));
            assertLogMessage(logFileReader, "Hello bar", 30000);
        }
    }

    static void assertLogMessage(BufferedReader logFileReader, String msg, long timeout) throws IOException {
        boolean found = false;
        final long deadline = System.currentTimeMillis() + timeout;
        while (System.currentTimeMillis() < deadline) {
            final String line = logFileReader.readLine();
            if (line == null) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            } else if (line.contains(msg)) {
                found = true;
                break;
            }
        }
        Assertions.assertTrue(found, "Could not find '" + msg + "' in " + LOG_FILE);
    }

    public static Asset applicationProperties() {
        Writer writer = new StringWriter();
        Properties props = new Properties();
        props.setProperty("quarkus.log.file.enable", "true");
        props.setProperty("quarkus.log.file.path", LOG_FILE.toString());
        props.setProperty("quarkus.log.file.rotation.rotate-on-boot", "false");
        try {
            props.store(writer, "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new StringAsset(writer.toString());
    }
}
