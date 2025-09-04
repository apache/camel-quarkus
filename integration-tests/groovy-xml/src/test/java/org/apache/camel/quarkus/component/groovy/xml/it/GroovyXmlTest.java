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
package org.apache.camel.quarkus.component.groovy.xml.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

@QuarkusTest
class GroovyXmlTest {

    private static final String BOOKS = """
            <library>
              <book id="bk101">
                <title>No Title</title>
                <author>F. Scott Fitzgerald</author>
                <year>1925</year>
                <genre>Classic</genre>
              </book>
              <book id="bk102">
                <title>1984</title>
                <author>George Orwell</author>
                <year>1949</year>
                <genre>Dystopian</genre>
              </book>
            </library>
            """;

    @Test
    public void testUnmarshal() throws Exception {

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(BOOKS)
                .post("/groovy-xml/unmarshal") //
                .then()
                .statusCode(200)
                .body(org.hamcrest.Matchers.containsString("bk101"))
                .body(org.hamcrest.Matchers.containsString("bk102"));
    }

    @Test
    public void testMarshal() throws Exception {
        //it is not possible to send Node, therefore conversion from xml to Node is done on Resource instance
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(BOOKS)
                .post("/groovy-xml/marshal") //
                .then()
                .statusCode(200)
                .body(Matchers.is(BOOKS));
    }

}
