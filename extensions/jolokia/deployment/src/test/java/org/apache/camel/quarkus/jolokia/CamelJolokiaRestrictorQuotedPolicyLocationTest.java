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

import io.quarkus.test.QuarkusExtensionTest;
import org.apache.camel.quarkus.jolokia.restrictor.CamelJolokiaRestrictor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The policy location is a key of the additional-properties map, so it may be written with or without quotes.
 * Both forms must be honoured by the restrictor.
 */
class CamelJolokiaRestrictorQuotedPolicyLocationTest {

    private static final String JOLOKIA_ACCESS_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <restrict>
                <remote>
                    <host>10.0.0.0/8</host>
                </remote>
            </restrict>
            """;

    static final File POLICY_FILE;

    static {
        try {
            POLICY_FILE = Files.createTempFile("jolokia-access", ".xml").toFile();
            POLICY_FILE.deleteOnExit();
            Files.writeString(POLICY_FILE.toPath(), JOLOKIA_ACCESS_XML);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @RegisterExtension
    static final QuarkusExtensionTest CONFIG = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.camel.jolokia.additional-properties.\"policyLocation\"",
                    POLICY_FILE.toURI().toString());

    @Test
    void quotedPolicyLocationIsLoaded() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isRemoteAccessAllowed("10.0.0.1"));
        assertFalse(restrictor.isRemoteAccessAllowed("192.168.1.1"));
    }
}
