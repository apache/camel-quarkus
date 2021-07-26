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
package org.apache.camel.quarkus.component.json.path.it;

import java.util.Arrays;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.quarkus.component.json.path.it.CarsRequest.Car;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class JsonPathTransformTest {

    private CarsRequest carsRequest;

    @BeforeEach
    public void setup() {
        carsRequest = new CarsRequest();
        Car redCar = new Car();
        redCar.setColor("red");
        Car greenCar = new Car();
        greenCar.setColor("green");
        carsRequest.setCars(Arrays.asList(redCar, greenCar));
    }

    //@Test
    public void getAllCarColorsShouldSucceed() {
        String colors = RestAssured.given() //
                .contentType(ContentType.JSON).body(carsRequest).get("/jsonpath/getAllCarColors").then().statusCode(200)
                .extract().body().asString();

        assertEquals("[red, green]", colors);
    }

}
