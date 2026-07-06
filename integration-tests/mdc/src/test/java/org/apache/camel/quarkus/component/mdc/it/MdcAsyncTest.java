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
package org.apache.camel.quarkus.component.mdc.it;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@QuarkusTest
@TestProfile(MdcAsyncTestProfile.class)
class MdcAsyncTest {

    @Test
    void testAsyncProcessingWithMdcPropagation() {
        String response = RestAssured.get("/mdc/async")
                .then()
                .statusCode(200)
                .body(containsString("asyncHeader:asyncValue"))
                .body(containsString("asyncProp:asyncPropValue"))
                .body(containsString("threadId:"), not(containsString("threadId:null")))
                .extract().asString();

        // MDC threadId on the async thread must differ from the calling thread
        assertNotEquals(extractValue(response, "threadId"), extractValue(response, "callingThread"));
    }

    private static String extractValue(String response, String key) {
        for (String line : response.split("\n")) {
            if (line.startsWith(key + ":")) {
                return line.substring(key.length() + 1);
            }
        }
        return null;
    }
}
