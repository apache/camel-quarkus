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
 * A final {@code @Ingest} method cannot be overridden by the ArC client proxy, so on a
 * normal-scoped bean it would silently run against the proxy's null fields — the same failure a
 * private method causes, and it gets the same build-time rejection.
 */
class IngestBuilderFinalMethodTest {

    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(Pipelines.class))
            .assertException(t -> ValidationTestSupport.assertFailure(t,
                    "must not be final, nor declared on a final class"));

    @Test
    void buildMustFail() {
        Assertions.fail("The build was expected to fail");
    }

    @ApplicationScoped
    public static class Pipelines {

        @Ingest("final-docs")
        final IngestPipeline finalDocs() {
            return IngestPipeline.from(Source.file("target/final-docs"));
        }
    }
}
