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
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.apache.camel.quarkus.component.geocoder.it.GeocoderGoogleResource.GOOGLE_GEOCODER_API_KEY;
import static org.hamcrest.Matchers.hasKey;

@QuarkusTest
@TestHTTPEndpoint(GeocoderGoogleResource.class)
class GeocoderGoogleTest {

    private static boolean enabled;

    @BeforeAll
    public static void setup() {
        // enables the integration tests if an API key exists
        enabled = ConfigProvider.getConfig()
                .getOptionalValue(GOOGLE_GEOCODER_API_KEY, String.class).isPresent();
    }

    @Test
    public void loadCurrentLocation() {
        // disable test if no API KEY
        if (enabled) {
            RestAssured.get()
                    .then()
                    .statusCode(200)
                    .body("[0]", hasKey("addressComponents"));
        }
    }

    @Test
    public void loadAddress() {
        if (enabled) {
            RestAssured.get("/address/calle marie curie, sevilla, sevilla")
                    .then()
                    .statusCode(200)
                    .body("[0]", hasKey("addressComponents"));
        }
    }

    @Test
    public void loadLatLong() {
        if (enabled) {
            RestAssured.get("/lat/40.714224/lon/-73.961452")
                    .then()
                    .statusCode(200)
                    .body("[0]", hasKey("addressComponents"));
        }
    }

}
