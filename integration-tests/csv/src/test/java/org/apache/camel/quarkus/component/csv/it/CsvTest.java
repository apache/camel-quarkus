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
package org.apache.camel.quarkus.component.csv.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import static org.hamcrest.Matchers.is;

@QuarkusTest
class CsvTest {

    //@Test
    public void json2csv() {
        RestAssured.given() //
                .contentType(ContentType.JSON)
                .accept(ContentType.TEXT)
                .body("[{\"name\":\"Melwah\", \"species\":\"Camelus Dromedarius\"},{\"name\":\"Al Hamra\", \"species\":\"Camelus Dromedarius\"}]")
                .post("/csv/json-to-csv")
                .then()
                .statusCode(200)
                .body(is("Melwah,Camelus Dromedarius\r\nAl Hamra,Camelus Dromedarius\r\n"));
    }

    //@Test
    public void csv2json() {
        RestAssured.given() //
                .contentType(ContentType.TEXT)
                .accept(ContentType.JSON)
                .body("Melwah,Camelus Dromedarius\r\nAl Hamra,Camelus Dromedarius\r\n")
                .post("/csv/csv-to-json")
                .then()
                .statusCode(200)
                .body(is("[[\"Melwah\",\"Camelus Dromedarius\"],[\"Al Hamra\",\"Camelus Dromedarius\"]]"));
    }

}
