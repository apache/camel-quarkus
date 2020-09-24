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
package org.apache.camel.quarkus.component.geocoder.it;

import org.apache.camel.component.geocoder.GeocoderStatus;

public class GeocoderResult {

    private GeocoderStatus status;
    private String lat;
    private String lng;
    private String latLng;
    private String address;
    private Country country;
    private String city;
    private String postalCode;
    private Region region;

    public GeocoderResult() {
    }

    public GeocoderResult withLat(String lat) {
        this.lat = lat;
        return this;
    }

    public GeocoderResult withLng(String lng) {
        this.lng = lng;
        return this;
    }

    public GeocoderResult withLatLng(String latLng) {
        this.latLng = latLng;
        return this;
    }

    public GeocoderResult withAddress(String address) {
        this.address = address;
        return this;
    }

    public GeocoderResult withStatus(GeocoderStatus status) {
        this.status = status;
        return this;
    }

    public GeocoderResult withCountry(String shortCode, String longCode) {
        this.country = new Country(shortCode, longCode);
        return this;
    }

    public GeocoderResult withCity(String city) {
        this.city = city;
        return this;
    }

    public GeocoderResult withPostalCode(String postalCode) {
        this.postalCode = postalCode;
        return this;
    }

    public GeocoderResult withRegion(String code, String name) {
        this.region = new Region(code, name);
        return this;
    }

    public GeocoderStatus getStatus() {
        return status;
    }

    public String getLat() {
        return lat;
    }

    public String getLng() {
        return lng;
    }

    public String getLatLng() {
        return latLng;
    }

    public String getAddress() {
        return address;
    }

    public Country getCountry() {
        return country;
    }

    public String getCity() {
        return city;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public Region getRegion() {
        return region;
    }
}
