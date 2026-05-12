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
package org.apache.camel.quarkus.component.seda.it;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import static org.hamcrest.Matchers.containsString;

@TestProfile(SedaVirtualThreadsTest.VirtualThreadsTestProfile.class)
@QuarkusTest
class SedaVirtualThreadsTest {
    @EnabledForJreRange(min = JRE.JAVA_21)
    @Test
    void sedaExecutesOnVirtualThread() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .post("/seda/virtualThreaded")
                .then()
                .statusCode(201);

        RestAssured.get("/seda/virtualThreadedResults")
                .then()
                .body(containsString("java.lang.VirtualThread"))
                .statusCode(200);
    }

    public static final class VirtualThreadsTestProfile implements QuarkusTestProfile {
        @Override
        public String getConfigProfile() {
            return "virtualThreads";
        }
    }
}
