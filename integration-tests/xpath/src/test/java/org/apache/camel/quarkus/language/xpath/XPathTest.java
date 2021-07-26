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
package org.apache.camel.quarkus.language.xpath;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import io.quarkus.test.junit.QuarkusTest;
import org.apache.commons.io.IOUtils;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@QuarkusTest
public class XPathTest {

    //@Test
    public void transformShouldSucceed() throws IOException {
        String xml = IOUtils.toString(XPathTest.class.getResourceAsStream("/students.xml"), StandardCharsets.UTF_8);
        given().body(xml).when().get("/xpath/transform").then().statusCode(200).body(is("ClausHadrian"));
    }

    //@Test
    public void whenPredicateShouldMatchWhenIdHasValueA() {
        String xml = "<body id='a'/>";
        given().body(xml).when().get("/xpath/choice").then().statusCode(200).body(is("A"));
    }

    //@Test
    public void concatCoreXPathFunctionShouldPrependFooBeforeMonica() {
        String xml = "<person name='Monica'/>";
        given().body(xml).when().get("/xpath/coreXPathFunctions").then().statusCode(200).body(is("foo-Monica"));
    }

    //@Test
    public void headerCamelXPathFunctionShouldMatchFooHeaderWhenValueIsBar() {
        String fooHeader = "bar";
        given().body(fooHeader).when().get("/xpath/camelXPathFunctions").then().statusCode(200).body(is("BAR"));
    }

    //@Test
    public void xpathLoadedFromResourceShouldSucceed() {
        String xml = "<person><name>Caroline</name></person>";
        given().body(xml).when().get("/xpath/resource").then().statusCode(200).body(is("Caroline"));
    }

    //@Test
    public void xpathAnnotationAppliedOnPriceBeanMethodParameterShouldSucceed() {
        String xml = "<env:Envelope xmlns:env='http://www.w3.org/2003/05/soap-envelope'><env:Body><price>38</price></env:Body></env:Envelope>";
        given().body(xml).when().get("/xpath/annotation").then().statusCode(200).body(is("38.0€"));
    }

    //@Test
    public void xpathWithPropertiesFunctionShouldMatchWhenTypeHeaderHasValueCamel() {
        String typeHeaderValue = "Camel";
        given().body(typeHeaderValue).when().get("/xpath/properties").then().statusCode(200).body(is("FOO"));
    }

    //@Test
    public void xpathWithSimpleFunctionEvaluatingBarPropertyShouldMatchWhenNameIsKong() {
        String xml = "<name>Kong</name>";
        given().body(xml).when().get("/xpath/simple").then().statusCode(200).body(is("BAR"));
    }

}
