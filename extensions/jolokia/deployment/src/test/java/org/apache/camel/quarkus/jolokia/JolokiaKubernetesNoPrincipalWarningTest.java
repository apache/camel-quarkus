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
package org.apache.camel.quarkus.jolokia;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;

import io.quarkus.test.QuarkusExtensionTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JolokiaKubernetesNoPrincipalWarningTest {

    static final File CA_CERT;

    static {
        try {
            CA_CERT = Files.createTempFile("fake-ca", ".crt").toFile();
            CA_CERT.deleteOnExit();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @RegisterExtension
    static final QuarkusExtensionTest CONFIG = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("kubernetes.service.host", "fake-host")
            .overrideConfigKey("quarkus.camel.jolokia.kubernetes.service-ca-cert", CA_CERT.getAbsolutePath())
            .setLogRecordPredicate(record -> record.getLevel().equals(Level.WARNING))
            .assertLogRecords(records -> assertTrue(
                    records.stream().anyMatch(r -> r.getMessage().contains("no client principal is configured")),
                    "Expected a warning about missing client principal"));

    @Test
    void applicationStartsWithClientPrincipalWarning() {
        // The assertLogRecords callback above verifies the warning was logged.
        // The Jolokia server is configured with HTTPS (due to the fake CA cert),
        // so we cannot make a plain HTTP request — app startup alone is sufficient.
    }
}
