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

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
@TestHTTPEndpoint(GeocoderNominationResource.class)
public class GeocoderNominationTest {

    @Test
    public void loadAddress() {
        RestAssured.get("/address/calle marie curie, sevilla, sevilla")
                .then()
                .statusCode(200)
                .body("status", equalTo("OK"))
                .body("postalCode", equalTo("41092"))
                .body("city", equalTo("Sevilla"))
                .body("country.shortCode", equalTo("ES"));
    }

    @Test
    public void loadLatLong() {
        RestAssured.get("/lat/40.714224/lon/-73.961452")
                .then()
                .statusCode(200)
                .body("status", equalTo("OK"))
                .body("postalCode", equalTo("11211"))
                .body("city", equalTo("New York"))
                .body("country.shortCode", equalTo("US"));
    }
}
