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

import io.quarkus.test.QuarkusExtensionTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/** The modality and content-type rules are validated at build time on the configuration path. */
class IngestModalityConfigTest {

    @RegisterExtension
    static final QuarkusExtensionTest CONFIG = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> {
            })
            .overrideConfigKey("quarkus.camel.langchain4j.ingest.unknown.modality", "sound")
            .overrideConfigKey("quarkus.camel.langchain4j.ingest.parsed.modality", "media")
            .overrideConfigKey("quarkus.camel.langchain4j.ingest.parsed.parser", "tika")
            .overrideConfigKey("quarkus.camel.langchain4j.ingest.split.modality", "MEDIA")
            .overrideConfigKey("quarkus.camel.langchain4j.ingest.split.document-splitter", "splitter")
            .overrideConfigKey("quarkus.camel.langchain4j.ingest.typed.content-type", "audio/wav")
            .overrideConfigKey("quarkus.camel.langchain4j.ingest.params.modality", "media")
            .overrideConfigKey("quarkus.camel.langchain4j.ingest.params.content-type", "; codecs=opus")
            .assertException(t -> ValidationTestSupport.assertFailure(t,
                    "'unknown': modality must be 'text' or 'media' (got 'sound')",
                    "'parsed' sets modality 'media' together with a parser",
                    "'split' sets modality 'media' together with a document-splitter",
                    "'typed': content-type only applies to modality 'media' (got 'audio/wav')",
                    "'params': content-type must not be blank or only parameters (got '; codecs=opus')"));

    @Test
    void buildMustFail() {
        Assertions.fail("The build was expected to fail");
    }
}
