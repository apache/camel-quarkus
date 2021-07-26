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
package org.apache.camel.quarkus.component.sap.netweaver.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Assertions;

import static org.hamcrest.Matchers.containsString;

@QuarkusTest
@QuarkusTestResource(SapNetweaverTestResource.class)
class SapNetweaverTest {

    //@Test
    public void testSapNetweaverJson() {
        final int port = ConfigProvider.getConfig().getValue("camel.netty.test-port", Integer.class);
        RestAssured.given()
                .queryParam("test-port", port)
                .get("/sap-netweaver/json")
                .then()
                .statusCode(200)
                .body(containsString("PRICE=422.94, CURRENCY=USD"));
    }

    //@Test
    public void testSapNetweaverXml() {
        final int port = ConfigProvider.getConfig().getValue("camel.netty.test-port", Integer.class);
        String body = RestAssured.given()
                .queryParam("test-port", port)
                .get("/sap-netweaver/xml")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        Assertions.assertTrue(body.contains("<d:PRICE>422.94</d:PRICE>"));
        Assertions.assertTrue(body.contains("<d:CURRENCY>USD</d:CURRENCY>"));
    }
}
