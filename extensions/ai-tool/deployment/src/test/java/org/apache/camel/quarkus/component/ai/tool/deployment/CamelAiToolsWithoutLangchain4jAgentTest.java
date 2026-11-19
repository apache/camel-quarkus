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

import io.quarkus.test.QuarkusExtensionTest;
import org.apache.camel.quarkus.component.ai.tool.CamelAiTools;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * With Quarkus LangChain4j present but camel-langchain4j-agent absent, the build logs exactly one warning that the
 * agent is not on the classpath. Checked in {@code assertLogRecords} of the static extension; the test method is only a
 * placeholder.
 */
class CamelAiToolsWithoutLangchain4jAgentTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TaggedService.class, OptionalDependencies.class))
            .setForcedDependencies(List.of(OptionalDependencies.QUARKUS_LANGCHAIN4J))
            .setLogRecordPredicate(r -> r.getMessage() != null && r.getMessage().contains("@CamelAiTools"))
            .assertLogRecords(records -> assertEquals(1, records.stream()
                    .filter(r -> r.getMessage().contains("camel-langchain4j-agent is not on the classpath"))
                    .count()));

    @Test
    void warnsAboutMissingLangchain4jAgent() {
        // asserted by assertLogRecords
    }

    @CamelAiTools("weather")
    public interface TaggedService {
    }
}
