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

public class Bike {
    private String make;
    private String modelNumber;
    private int year;
    private boolean electric;

    public Bike(String make, String modelNumber, int year, boolean electric) {
        this.make = make;
        this.modelNumber = modelNumber;
        this.year = year;
        this.electric = electric;
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
    }

    public String getModelNumber() {
        return modelNumber;
    }

    public void setModelNumber(String modelNumber) {
        this.modelNumber = modelNumber;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public boolean isElectric() {
        return electric;
    }

    public void setElectric(boolean electric) {
        this.electric = electric;
    }

    @Override
    public String toString() {
        return String.join(",", make, modelNumber, String.valueOf(year), String.valueOf(electric));
    }

    public static Bike fromString(String bikeString) {
        final String[] split = bikeString.split(",");
        return new Bike(split[0], split[1], Integer.parseInt(split[2]), Boolean.parseBoolean(split[3]));
    }
}
