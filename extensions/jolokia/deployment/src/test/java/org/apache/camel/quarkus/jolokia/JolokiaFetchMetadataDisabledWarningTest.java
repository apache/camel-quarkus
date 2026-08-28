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

import java.util.logging.Level;

import io.quarkus.test.QuarkusExtensionTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A request carrying neither an Origin nor a Referer header is accepted, and Jolokia's Sec-Fetch-* handling is
 * what prevents a browser from making one. Turning it off on an agent that other hosts can reach therefore
 * removes the only thing guarding that path, which is worth reporting at startup rather than only in the
 * documentation.
 */
class JolokiaFetchMetadataDisabledWarningTest {

    @RegisterExtension
    static final QuarkusExtensionTest CONFIG = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.camel.jolokia.server.host", "0.0.0.0")
            .overrideConfigKey("quarkus.camel.jolokia.additional-properties.\"useFetchMetadata\"", "false")
            .setLogRecordPredicate(record -> record.getLevel().equals(Level.WARNING))
            .assertLogRecords(records -> assertTrue(
                    records.stream().anyMatch(r -> r.getMessage().contains("Fetch Metadata handling is disabled")),
                    "Expected a warning that Fetch Metadata handling is disabled"));

    @Test
    void applicationStartsWithFetchMetadataWarning() {
        // The assertLogRecords callback above verifies the warning was logged.
    }
}
