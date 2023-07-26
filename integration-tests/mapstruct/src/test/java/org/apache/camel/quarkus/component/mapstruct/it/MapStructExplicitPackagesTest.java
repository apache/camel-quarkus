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
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import org.apache.camel.quarkus.component.mapstruct.it.mapper.car.CarMapper;
import org.apache.camel.quarkus.component.mapstruct.it.mapper.vehicle.VehicleMapper;
import org.apache.camel.quarkus.component.mapstruct.it.model.Car;
import org.apache.camel.quarkus.component.mapstruct.it.model.Employee;
import org.apache.camel.quarkus.component.mapstruct.it.model.EmployeeDto;
import org.apache.camel.quarkus.component.mapstruct.it.model.Vehicle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@TestProfile(MapStructExplicitPackagesTestProfile.class)
public class MapStructExplicitPackagesTest {
    @ParameterizedTest
    @ValueSource(strings = { "component", "converter" })
    void mapVehicleToCarSuccess(String value) {
        Vehicle vehicle = new Vehicle("Volvo", "XC60", "true", 2021);

        String response = RestAssured.given()
                .queryParam("fromTypeName", Vehicle.class.getName())
                .queryParam("toTypeName", Car.class.getName())
                .body(vehicle.toString())
                .post("/mapstruct/" + value)
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        Car car = Car.fromString(response);

        assertEquals(vehicle.getCompany(), car.getBrand());
        assertEquals(vehicle.getName(), car.getModel());
        assertEquals(vehicle.getYear(), car.getYear());
        assertEquals(Boolean.parseBoolean(vehicle.getPower()), car.isElectric());
    }

    @ParameterizedTest
    @ValueSource(strings = { "component", "converter" })
    void mapEmployeeToEmployeeDtoFail(String value) {
        Employee employee = new Employee();
        employee.setId(1);
        employee.setName("Mr Camel Quarkus");

        // Mapping should fail because the configured mapper packages do not handle employee types
        RestAssured.given()
                .queryParam("fromTypeName", Employee.class.getName())
                .queryParam("toTypeName", EmployeeDto.class.getName())
                .body(employee.toString())
                .post("/mapstruct/" + value)
                .then()
                .statusCode(500)
                .body(containsString("NoTypeConversionAvailableException"));
    }

    @Test
    void mapStructComponentPackages() {
        RestAssured.get("/mapstruct/component/packages")
                .then()
                .statusCode(200)
                .body(containsString(CarMapper.class.getPackageName()), containsString(VehicleMapper.class.getPackageName()));
    }
}
