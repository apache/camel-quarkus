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
package org.apache.camel.quarkus.component.salesforce;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.hamcrest.core.Is.is;

@EnabledIfEnvironmentVariable(named = "SALESFORCE_USERNAME", matches = ".+")
@EnabledIfEnvironmentVariable(named = "SALESFORCE_PASSWORD", matches = ".+")
@EnabledIfEnvironmentVariable(named = "SALESFORCE_CLIENTID", matches = ".+")
@EnabledIfEnvironmentVariable(named = "SALESFORCE_CLIENTSECRET", matches = ".+")
@QuarkusTest
class SalesforceTest {

    @Test
    public void testSalesforceComponent() {
        RestAssured.get("/salesforce/document/test")
                .then()
                .statusCode(200)
                .body("attributes.type", is("Document"));
    }
}
