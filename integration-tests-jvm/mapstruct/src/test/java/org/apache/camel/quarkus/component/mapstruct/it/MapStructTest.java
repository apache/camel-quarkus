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
package org.apache.camel.quarkus.component.mapstruct.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.camel.quarkus.component.mapstruct.it.model.Car;
import org.apache.camel.quarkus.component.mapstruct.it.model.Vehicle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
public class MapStructTest {
    private static final Vehicle VEHICLE = new Vehicle("Volvo", "XC60", "true", 2021);

    @ParameterizedTest
    @ValueSource(strings = { "component", "converter" })
    public void testMapping(String value) {
        String response = RestAssured.given()
                .body(VEHICLE.toString())
                .post("/mapstruct/" + value)
                .then()
                .statusCode(200)
                .extract().body().asString();

        Car car = Car.fromString(response);

        assertEquals(car.getBrand(), VEHICLE.getCompany());
        assertEquals(car.getModel(), VEHICLE.getName());
        assertEquals(car.getYear(), VEHICLE.getYear());
        assertEquals(car.isElectric(), Boolean.parseBoolean(VEHICLE.getPower()));
    }
}
