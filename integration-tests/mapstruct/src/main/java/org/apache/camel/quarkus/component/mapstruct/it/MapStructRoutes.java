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

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.quarkus.component.mapstruct.it.model.Bike;
import org.apache.camel.quarkus.component.mapstruct.it.model.Car;
import org.apache.camel.quarkus.component.mapstruct.it.model.CarDto;
import org.apache.camel.quarkus.component.mapstruct.it.model.Cat;
import org.apache.camel.quarkus.component.mapstruct.it.model.Dog;
import org.apache.camel.quarkus.component.mapstruct.it.model.Employee;
import org.apache.camel.quarkus.component.mapstruct.it.model.EmployeeDto;
import org.apache.camel.quarkus.component.mapstruct.it.model.Vehicle;

public class MapStructRoutes extends RouteBuilder {
    @Override
    public void configure() throws Exception {
        from("direct:component")
                .toD("mapstruct:${header.toType}");

        from("direct:converter")
                .choice()
                .when(simple("${header.toType} == 'bike'"))
                .convertBodyTo(Bike.class)
                .when(simple("${header.toType} == 'car'"))
                .convertBodyTo(Car.class)
                .when(simple("${header.toType} == 'cardto'"))
                .convertBodyTo(CarDto.class)
                .when(simple("${header.toType} == 'cat'"))
                .convertBodyTo(Cat.class)
                .when(simple("${header.toType} == 'dog'"))
                .convertBodyTo(Dog.class)
                .when(simple("${header.toType} == 'employee'"))
                .convertBodyTo(Employee.class)
                .when(simple("${header.toType} == 'employeedto'"))
                .convertBodyTo(EmployeeDto.class)
                .when(simple("${header.toType} == 'vehicle'"))
                .convertBodyTo(Vehicle.class);
    }
}
