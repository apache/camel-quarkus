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
package org.apache.camel.quarkus.component.openai.it;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.util.FileUtil;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static org.hamcrest.Matchers.containsStringIgnoringCase;

@QuarkusTestResource(OpenaiTestResource.class)
@QuarkusTest
class OpenaiAudioTest {

    private static final Logger LOG = Logger.getLogger(OpenaiAudioTest.class);

    @BeforeEach
    void logTestName(TestInfo testInfo) {
        LOG.info(String.format("Running OpenaiAudioTest test %s", testInfo.getDisplayName()));
    }

    @Test
    void audioTranscription() throws IOException {
        Path audioFile = Paths.get("target/test-audio.wav");

        try (InputStream stream = OpenaiAudioTest.class.getResourceAsStream("/audio/test-audio.wav")) {
            if (stream == null) {
                throw new IllegalStateException("Failed loading test-audio.wav");
            }

            try (OutputStream out = new FileOutputStream(audioFile.toFile())) {
                stream.transferTo(out);
            }

            RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .body(audioFile.toAbsolutePath().toString())
                    .post("/openai/audio/transcription")
                    .then()
                    .statusCode(200)
                    .body(containsStringIgnoringCase("hello"));
        } finally {
            if (FileUtil.isWindows()) {
                audioFile.toFile().deleteOnExit();
            } else {
                Files.deleteIfExists(audioFile);
            }
        }
    }

}
