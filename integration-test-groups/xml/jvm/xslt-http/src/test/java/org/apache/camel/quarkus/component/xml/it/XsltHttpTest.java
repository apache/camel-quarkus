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

import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.DisabledOnIntegrationTest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
@WithTestResource(XmlHttpTestResource.class)
class XsltHttpTest {
    private static final String BODY = "<mail><subject>Hey</subject><body>Hello world!</body></mail>";

    @Test
    @DisabledOnIntegrationTest("Generating xslt templates dynamically does not be supported in native mode")
    public void xsltBean() {
        final String actual = RestAssured.given()
                .body(BODY)
                .post("/xml/xslt-http")
                .then()
                .statusCode(200)
                .extract().body().asString().trim().replaceAll(">\\s+<", "><");

        Assertions.assertEquals(
                "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><classpath-xsl subject=\"Hey\"><cheese><mail><subject>Hey</subject><body>Hello world!</body></mail></cheese></classpath-xsl>",
                actual);
    }
}
