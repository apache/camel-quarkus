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

import io.quarkus.test.QuarkusUnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Fetch Metadata warning is about a browser reaching the agent, so there is nothing to warn about while the
 * agent is bound to loopback. Without this the warning would fire on every local development setup that turns
 * the headers off, and a warning that appears when there is no problem is one nobody reads.
 */
class JolokiaFetchMetadataDisabledOnLoopbackTest {

    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.camel.jolokia.server.host", "localhost")
            .overrideConfigKey("quarkus.camel.jolokia.additional-properties.\"useFetchMetadata\"", "false")
            .setLogRecordPredicate(record -> record.getLevel().equals(Level.WARNING))
            .assertLogRecords(records -> assertTrue(
                    records.stream().noneMatch(r -> r.getMessage().contains("Fetch Metadata handling is disabled")),
                    "Expected no Fetch Metadata warning while the agent is bound to loopback"));

    @Test
    void applicationStartsWithoutFetchMetadataWarning() {
        // The assertLogRecords callback above verifies no warning was logged.
    }
}
