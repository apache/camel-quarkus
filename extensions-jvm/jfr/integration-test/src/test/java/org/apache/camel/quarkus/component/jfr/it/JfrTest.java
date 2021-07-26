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
package org.apache.camel.quarkus.component.jfr.it;

import java.io.File;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(JfrTestResource.class)
class JfrTest {

    //@Test
    public void testflightRecorderRecording() {
        // Make sure the flight recorder is configured on the camel context
        RestAssured.when().get("/jfr/startup-step-recorder").then().body(is("true"));

        // Verify recording file exists
        File recordingsDir = JfrTestResource.JFR_RECORDINGS_DIR.toFile();
        String[] recordings = recordingsDir.list();
        assertEquals(1, recordings.length);

        String fileName = recordings[0];
        assertTrue(fileName.matches("camel-recording[0-9]+\\.jfr"));
    }
}
