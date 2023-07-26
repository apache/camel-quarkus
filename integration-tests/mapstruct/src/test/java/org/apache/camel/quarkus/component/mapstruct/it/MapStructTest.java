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
import org.apache.camel.quarkus.component.mapstruct.CamelQuarkusMapStructMapperFinder;
import org.apache.camel.quarkus.component.mapstruct.it.mapper.car.CarMapper;
import org.apache.camel.quarkus.component.mapstruct.it.mapper.cat.CatMapper;
import org.apache.camel.quarkus.component.mapstruct.it.mapper.dog.DogMapper;
import org.apache.camel.quarkus.component.mapstruct.it.mapper.employee.EmployeeMapper;
import org.apache.camel.quarkus.component.mapstruct.it.mapper.vehicle.VehicleMapper;
import org.apache.camel.quarkus.component.mapstruct.it.model.Bike;
import org.apache.camel.quarkus.component.mapstruct.it.model.Car;
import org.apache.camel.quarkus.component.mapstruct.it.model.Cat;
import org.apache.camel.quarkus.component.mapstruct.it.model.Dog;
import org.apache.camel.quarkus.component.mapstruct.it.model.Employee;
import org.apache.camel.quarkus.component.mapstruct.it.model.EmployeeDto;
import org.apache.camel.quarkus.component.mapstruct.it.model.Vehicle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
public class MapStructTest {
    @ParameterizedTest
    @ValueSource(strings = { "component", "converter" })
    void mapVehicleToCar(String value) {
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
    void mapBikeToCar(String value) {
        Bike bike = new Bike("Honda", "CBR65R", 2023, false);

        String response = RestAssured.given()
                .queryParam("fromTypeName", Bike.class.getName())
                .queryParam("toTypeName", Car.class.getName())
                .body(bike.toString())
                .post("/mapstruct/" + value)
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        Car car = Car.fromString(response);

        assertEquals(bike.getMake(), car.getBrand());
        assertEquals(bike.getModelNumber(), car.getModel());
        assertEquals(bike.getYear(), car.getYear());
        assertEquals(bike.isElectric(), car.isElectric());
    }

    @ParameterizedTest
    @ValueSource(strings = { "component", "converter" })
    void mapDogToCatWithAlternatePackageAndClassName(String value) {
        Dog dog = new Dog("1", "Snoopy", 8, "bark");

        String response = RestAssured.given()
                .queryParam("fromTypeName", Dog.class.getName())
                .queryParam("toTypeName", Cat.class.getName())
                .body(dog.toString())
                .post("/mapstruct/" + value)
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        Cat cat = Cat.fromString(response);

        assertEquals(dog.getDogId(), cat.getCatId());
        assertEquals(dog.getName(), cat.getName());
        assertEquals(dog.getAge(), cat.getAge());
        assertEquals("meow", cat.getSound());
    }

    @ParameterizedTest
    @ValueSource(strings = { "component", "converter" })
    void mapCatToDogWithApplicationScopedMapperBean(String value) {
        Cat cat = new Cat("1", "Garfield", 12, "meow");

        String response = RestAssured.given()
                .queryParam("fromTypeName", Cat.class.getName())
                .queryParam("toTypeName", Dog.class.getName())
                .body(cat.toString())
                .post("/mapstruct/" + value)
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        Dog dog = Dog.fromString(response);

        assertEquals(cat.getCatId(), dog.getDogId());
        assertEquals(cat.getName(), dog.getName());
        assertEquals(cat.getAge(), dog.getAge());
        assertEquals("bark", dog.getVocalization());
    }

    @ParameterizedTest
    @ValueSource(strings = { "component", "converter" })
    void mapEmployeeToEmployeeDtoWithAlternateClassName(String value) {
        Employee employee = new Employee();
        employee.setId(1);
        employee.setName("Mr Camel Quarkus");

        String response = RestAssured.given()
                .queryParam("fromTypeName", Employee.class.getName())
                .queryParam("toTypeName", EmployeeDto.class.getName())
                .body(employee.toString())
                .post("/mapstruct/" + value)
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        EmployeeDto dto = EmployeeDto.fromString(response);
        assertEquals(employee.getId(), dto.getEmployeeId());
        assertEquals(employee.getName(), dto.getEmployeeName());
    }

    @ParameterizedTest
    @ValueSource(strings = { "component", "converter" })
    void mapEmployeeDtoToEmployeeWithInheritedMapperMethod(String value) {
        EmployeeDto dto = new EmployeeDto();
        dto.setEmployeeId(1);
        dto.setEmployeeName("Mr Camel Quarkus");

        String response = RestAssured.given()
                .queryParam("fromTypeName", EmployeeDto.class.getName())
                .queryParam("toTypeName", Employee.class.getName())
                .body(dto.toString())
                .post("/mapstruct/" + value)
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        Employee employee = Employee.fromString(response);
        assertEquals(dto.getEmployeeId(), employee.getId());
        assertEquals(dto.getEmployeeName(), employee.getName());
    }

    @ParameterizedTest
    @ValueSource(strings = { "component", "converter" })
    void mapCarToVehicleUsingStaticFieldMapper(String value) {
        Car car = new Car("Volvo", "XC60", 2021, true);

        String response = RestAssured.given()
                .queryParam("fromTypeName", Car.class.getName())
                .queryParam("toTypeName", Vehicle.class.getName())
                .body(car.toString())
                .post("/mapstruct/" + value)
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        Vehicle vehicle = Vehicle.fromString(response);

        assertEquals(car.getBrand(), vehicle.getCompany());
        assertEquals(car.getModel(), vehicle.getName());
        assertEquals(car.getYear(), vehicle.getYear());
        assertEquals(car.isElectric(), Boolean.parseBoolean(vehicle.getPower()));
    }

    @Test
    void mapStructMapperFinderImpl() {
        RestAssured.get("/mapstruct/finder/mapper")
                .then()
                .statusCode(200)
                .body(is(CamelQuarkusMapStructMapperFinder.class.getName()));
    }

    @Test
    void mapStructComponentPackages() {
        RestAssured.get("/mapstruct/component/packages")
                .then()
                .statusCode(200)
                .body(
                        containsString(CarMapper.class.getPackageName()),
                        containsString(CatMapper.class.getPackageName()),
                        containsString(DogMapper.class.getPackageName()),
                        containsString(EmployeeMapper.class.getPackageName()),
                        containsString(VehicleMapper.class.getPackageName()));
    }
}
