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
package org.apache.camel.quarkus.component.xml.it;

import io.quarkus.test.junit.DisabledOnIntegrationTest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class XsltAggregateTest {

    @Test
    @DisabledOnIntegrationTest("Generating xslt templates dynamically does not be supported in native mode")
    public void aggregate() {
        final String actual = RestAssured.given()
                .accept(ContentType.TEXT)
                .get("/xml/aggregate")
                .then()
                .statusCode(200)
                .extract().body().asString().trim().replaceAll(">\\s+<", "><");

        Assertions.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><item>ABC</item>", actual);
    }
}
