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
package org.apache.camel.quarkus.variables.it;

import java.util.Collections;
import java.util.Set;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Test is executed in JVM mode only because the profile enabling customVariableRepository can not be enabled for
 * this class in the native (and stay disabled for the other test class).
 * I think that it is not necessary to create a module for this test only, because it uses code tested by other tests
 * and
 * there is no reason to believe, that it fails in native.
 */
@QuarkusTest
@TestProfile(CustomRepositoryVariablesTest.Profile.class)
class CustomRepositoryVariablesTest {

    @Test
    public void testCustomGlobalRepository() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .post("/variables/customGlobalRepository")
                .then()
                .statusCode(200)
                .body(Matchers.is("null,!" + VariablesRoutes.VARIABLE_VALUE + "!"));
    }

    public static class Profile implements QuarkusTestProfile {

        @Override
        public Set<Class<?>> getEnabledAlternatives() {
            return Collections.singleton(VariablesProducers.MyGlobalRepo.class);
        }
    }

}
