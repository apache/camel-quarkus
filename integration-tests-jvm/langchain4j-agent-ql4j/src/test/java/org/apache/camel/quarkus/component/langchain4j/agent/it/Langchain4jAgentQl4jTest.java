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
package org.apache.camel.quarkus.component.langchain4j.agent.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import org.apache.camel.quarkus.component.langchain4j.agent.ql4j.it.Langchain4jAgentQl4jProfile;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.Matchers.*;

@ExtendWith(Langchain4jTestWatcher.class)
@QuarkusTestResource(Langchain4jAgentTestResource.class)
@TestProfile(Langchain4jAgentQl4jProfile.class)
@QuarkusTest
@Disabled //because of Native Library /home/jondruse/.djl.ai/tokenizers/0.20.3-0.31.1-cpu-linux-x86_64/libtokenizers.so already loaded in another classloader
//but class is present because on the future investigation, it could help to have it
//works if executed separately (only this test class) and with wire mock
class Langchain4jAgentQl4jTest {

    @Test
    void simpleUserMessage() {
        //when the rag with <default> scope is present, the request contains rag information and therefore wired mock does not exist
        RestAssured.given()
                .body(Langchain4jAgentTest.TEST_USER_MESSAGE_SIMPLE)
                .post("/langchain4j-agent/simple")
                .then()
                .statusCode(500);
    }

}
