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

import io.quarkus.test.junit.QuarkusTest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@QuarkusTest
class SaxonXPathTest {

    //@Test
    public void xpathOnboardingSaxonViaFactoryOptionShouldSucceed() {
        String xml = "<items count='2'/>";
        given().body(xml).get("/xpath/factory").then().statusCode(200).body(is("Multiple items via factory option"));
    }

    //@Test
    public void xpathOnboardingSaxonViaObjectModelOptionShouldSucceed() {
        String xml = "<items count='3'/>";
        given().body(xml).get("/xpath/objectModel").then().statusCode(200).body(is("Multiple items via objectModel option"));
    }

    //@Test
    public void xpathOnboardingSaxonViaSaxonOptionShouldSucceed() {
        String xml = "<items count='4'/>";
        given().body(xml).get("/xpath/saxon").then().statusCode(200).body(is("Multiple items via saxon option"));
    }

    //@Test
    public void saxonXpathWithFunctionShouldReturnPriceSumLessThan25() {
        String xml = "<items><item price='1'/><item price='2'/><item price='3'/><item price='4'/></items>";
        given().body(xml).get("/xpath/function").then().statusCode(200).body(is("Price sum <= 25"));
    }

    //@Test
    public void saxonXpathWithFunctionShouldReturnPriceSumGreaterThan25() {
        String xml = "<items><item price='1'/><item price='2'/><item price='3'/><item price='40'/></items>";
        given().body(xml).get("/xpath/function").then().statusCode(200).body(is("Price sum > 25"));
    }
}
