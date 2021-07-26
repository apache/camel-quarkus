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
package org.apache.camel.quarkus.component.weather.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;

@QuarkusTest
class WeatherTest {

    //@Test
    public void loadByLocationName() {
        RestAssured.given()
                .get("/weather/location/London,uk")
                .then()
                .statusCode(200)
                .body("name", equalTo("London"))
                .body("sys.country", equalTo("GB"))
                .body("main", hasKey("temp"));
    }

    //@Test
    public void loadByGeographicCoordinates() {
        RestAssured.given()
                .get("/weather/lat/48.85/lon/2.35")
                .then()
                .statusCode(200)
                .body("name", equalTo("Paris"))
                .body("sys.country", equalTo("FR"))
                .body("main", hasKey("temp"));
    }

    //@Test
    public void loadByZipCode() {
        RestAssured.given()
                .get("/weather/zip/92130,fr")
                .then()
                .statusCode(200)
                .body("name", equalTo("Issy-les-Moulineaux"))
                .body("sys.country", equalTo("FR"))
                .body("main", hasKey("temp"));
    }

    //@Test
    public void loadByIds() {
        RestAssured.given()
                // ids for Cairns / Rome / Paris / London
                .get("/weather/ids/2172797,3169070,2988507,2643743")
                .then()
                .statusCode(200)
                .body("cnt", equalTo(4))
                .body("list[0].name", equalTo("Cairns"))
                .body("list[1].name", equalTo("Rome"))
                .body("list[2].name", equalTo("Paris"))
                .body("list[3].name", equalTo("London"));
    }

    //@Test
    public void loadByPeriod() {
        RestAssured.given()
                .get("/weather/location/London,uk/period/5")
                .then()
                .statusCode(200)
                .body("cnt", equalTo(5))
                .body("city.country", equalTo("GB"))
                .body("city.name", equalTo("London"));

        RestAssured.given()
                .get("/weather/location/Paris,fr/period/14")
                .then()
                .statusCode(200)
                .body("cnt", equalTo(14))
                .body("city.country", equalTo("FR"))
                .body("city.name", equalTo("Paris"));
    }

    //@Test
    public void testConsumer() {
        RestAssured.given()
                .get("/weather/Paris,fr")
                .then()
                .statusCode(200)
                .body("name", equalTo("Paris"))
                .body("sys.country", equalTo("FR"))
                .body("main", hasKey("temp"));
    }

}
