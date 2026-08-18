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
package org.apache.camel.quarkus.component.langchain4j.ingest.deployment;

import io.quarkus.test.QuarkusUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * A consumer URI naming a component that is not on the classpath stops the build with the
 * add-extension command that fixes it.
 */
class IngestUnknownComponentTest {

    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .withApplicationRoot(jar -> {
            })
            .overrideConfigKey("quarkus.camel.ai.ingest.docs.source.uri", "nonexistent://feed")
            .assertException(t -> ValidationTestSupport.assertFailure(t,
                    "component 'nonexistent' is not on the classpath",
                    "quarkus:add-extension -Dextensions=camel-quarkus-nonexistent"));

    @Test
    void buildMustFail() {
        Assertions.fail("The build was expected to fail");
    }
}
