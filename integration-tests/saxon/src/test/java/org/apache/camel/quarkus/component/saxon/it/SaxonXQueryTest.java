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
package org.apache.camel.quarkus.component.saxon.it;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.apache.commons.io.IOUtils.resourceToString;
import static org.hamcrest.Matchers.is;

@QuarkusTest
class SaxonXQueryTest {

    @Test
    public void xqueyrFilterShouldMatchJames() {
        String xml = "<person name='James' city='London'/>";
        given().body(xml).get("/xquery/filter").then().statusCode(200).body(is("JAMES"));
    }

    @Test
    public void xqueryTransformShouldConcatEmployeeIdAndSuffix() throws IOException {
        String xml = resourceToString("myinput.xml", StandardCharsets.UTF_8, Thread.currentThread().getContextClassLoader());
        given().body(xml).get("/xquery/transform").then().statusCode(200).body(is("123Suffix"));
    }

    @Test
    public void xqueryFromResourceShouldReturnLondon() {
        String xml = "<person name='James' city='London'/>";
        given().body(xml).get("/xquery/resource").then().statusCode(200).body(is("London"));
    }

    @Test
    public void produceToXQueryComponentShouldTransformMessage() {
        String expectedXml = "<transformed subject=\"Hey\"><mail><subject>Hey</subject><body>Hello world!</body></mail></transformed>";
        String xml = "<mail><subject>Hey</subject><body>Hello world!</body></mail>";
        given().body(xml).get("/xquery/produce").then().statusCode(200).body(is(expectedXml));
    }

    @Test
    public void customExtensionShouldTransformMessage() {
        String expectedXml = "<transformed extension-function-render=\"arg1[test]\"/>";
        String xml = "<body>test</body>";
        given().body(xml).get("/xquery/extension").then().statusCode(200).body(is(expectedXml));
    }

    @Test
    public void xqueryAnnotationOnBeanParameterShouldExtractFooId() {
        String expected = "Foo id is 'bar'";
        String xml = "<foo id='bar'>hello</foo>";
        given().body(xml).get("/xquery/bean").then().statusCode(200).body(is(expected));
    }
}
