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
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.quarkus.component.langchain4j.ingest.Ingest;
import org.apache.camel.quarkus.component.langchain4j.ingest.IngestPipeline;
import org.apache.camel.quarkus.component.langchain4j.ingest.Source;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * {@code source.idempotent-repository} is owned by a builder-declared pipeline; configuring it
 * externally fails at startup like the rest of {@code source.*}.
 */
class IngestBuilderConfigIdempotentOverrideTest {

    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(Pipelines.class))
            .overrideConfigKey("quarkus.camel.langchain4j.ingest.docs.source.idempotent-repository", "external-repo")
            .assertException(t -> ValidationTestSupport.assertFailure(t,
                    "declared in Java", "Remove quarkus.camel.langchain4j.ingest.docs.source.*"));

    @Test
    void startMustFail() {
        Assertions.fail("The application start was expected to fail");
    }

    @ApplicationScoped
    public static class Pipelines {

        @Ingest("docs")
        IngestPipeline docs() {
            return IngestPipeline.from(Source.file("target/docs"));
        }
    }
}
