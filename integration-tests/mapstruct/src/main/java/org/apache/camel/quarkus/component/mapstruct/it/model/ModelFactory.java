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
package org.apache.camel.quarkus.component.mapstruct.it.model;

public class ModelFactory {

    public static Object getModel(String modelString, String modelClassName) {
        if (Bike.class.getName().equals(modelClassName)) {
            return Bike.fromString(modelString);
        } else if (Car.class.getName().equals(modelClassName)) {
            return Car.fromString(modelString);
        } else if (CarDto.class.getName().equals(modelClassName)) {
            return CarDto.fromString(modelString);
        } else if (Cat.class.getName().equals(modelClassName)) {
            return Cat.fromString(modelString);
        } else if (Dog.class.getName().equals(modelClassName)) {
            return Dog.fromString(modelString);
        } else if (Employee.class.getName().equals(modelClassName)) {
            return Employee.fromString(modelString);
        } else if (EmployeeDto.class.getName().equals(modelClassName)) {
            return EmployeeDto.fromString(modelString);
        } else if (Vehicle.class.getName().equals(modelClassName)) {
            return Vehicle.fromString(modelString);
        }
        throw new IllegalArgumentException("Unknown model class: " + modelClassName);
    }
}
