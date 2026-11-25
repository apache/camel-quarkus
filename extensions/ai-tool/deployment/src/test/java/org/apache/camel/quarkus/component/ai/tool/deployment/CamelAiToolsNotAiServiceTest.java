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
package org.apache.camel.quarkus.component.ai.tool.deployment;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import io.quarkus.test.QuarkusExtensionTest;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.quarkus.component.ai.tool.CamelAiTools;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@code @CamelAiTools} on a class that is not a {@code @RegisterAiService} interface has no effect, so the build
 * warns.
 */
class CamelAiToolsNotAiServiceTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(SupportFacade.class, OptionalDependencies.class))
            .setForcedDependencies(
                    List.of(OptionalDependencies.QUARKUS_LANGCHAIN4J, OptionalDependencies.CAMEL_LANGCHAIN4J_AGENT))
            .setLogRecordPredicate(r -> r.getLevel().equals(Level.WARNING)
                    && r.getMessage().contains("only applies to @RegisterAiService interfaces"))
            .assertLogRecords(records -> assertEquals(List.of(SupportFacade.class.getName()),
                    records.stream().map(CamelAiToolsNotAiServiceTest::annotatedClass).toList()));

    @Test
    void warnsForPlainBean() {
        // Asserted by assertLogRecords above
    }

    private static String annotatedClass(LogRecord record) {
        return String.valueOf(record.getParameters()[0]);
    }

    @ApplicationScoped
    @CamelAiTools("support")
    public static class SupportFacade {
    }
}
