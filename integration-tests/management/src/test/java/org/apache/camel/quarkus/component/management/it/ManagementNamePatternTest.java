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
package org.apache.camel.quarkus.component.management.it;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.apache.camel.quarkus.component.management.it.ManagementNamePatternTestProfile.MANAGEMENT_NAME_PREFIX;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@TestProfile(ManagementNamePatternTestProfile.class)
class ManagementNamePatternTest {

    @Test
    public void testManagementNamePattern() {
        String contextName = RestAssured.get("/management/context/name")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        String managementName = MANAGEMENT_NAME_PREFIX + contextName;

        // The CamelContext management name should be resolved from the configured pattern
        RestAssured.given()
                .queryParam("name", "org.apache.camel:type=context,*")
                .queryParam("attribute", "ManagementName")
                .get("/management/attribute")
                .then()
                .statusCode(200)
                .body(is(managementName));

        // Managed objects should be registered under the management name, and still refer to the original CamelContext name
        RestAssured.given()
                .queryParam("name", "org.apache.camel:context=" + managementName + ",type=routes,*")
                .queryParam("attribute", "CamelId")
                .get("/management/attribute")
                .then()
                .statusCode(200)
                .body(is(contextName));
    }
}
