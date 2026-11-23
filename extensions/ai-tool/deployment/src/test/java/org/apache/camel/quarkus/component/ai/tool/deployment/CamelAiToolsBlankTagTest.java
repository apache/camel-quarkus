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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * A {@code @CamelAiTools("")} interface (blank tag) must fail the build with "<class> has no tag". The assertion is in
 * {@code assertException} of the static extension; the test method is only reached if the build wrongly succeeds.
 */
class CamelAiToolsBlankTagTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(BlankTagService.class, OptionalDependencies.class))
            .setForcedDependencies(
                    List.of(OptionalDependencies.QUARKUS_LANGCHAIN4J, OptionalDependencies.CAMEL_LANGCHAIN4J_AGENT))
            .assertException(t -> assertTrue(rootMessages(t).contains(BlankTagService.class.getName() + " has no tag"),
                    "Unexpected build failure: " + rootMessages(t)));

    @Test
    void blankTagFailsTheBuild() {
        fail("The build must fail");
    }

    private static String rootMessages(Throwable t) {
        StringBuilder messages = new StringBuilder();
        for (Throwable cause = t; cause != null; cause = cause.getCause()) {
            messages.append(cause.getMessage()).append('\n');
        }
        return messages.toString();
    }

    @CamelAiTools("")
    public interface BlankTagService {
    }
}
