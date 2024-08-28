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

import com.github.tomakehurst.wiremock.WireMockServer;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.camel.quarkus.test.wiremock.MockServer;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.request;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.Matchers.hasKey;

@QuarkusTest
@TestHTTPEndpoint(GeocoderGoogleResource.class)
@QuarkusTestResource(GeocoderTestResource.class)
class GeocoderGoogleTest {

    @MockServer
    WireMockServer server;

    @Test
    public void loadCurrentLocation() {
        // We need to manually stub this API call because it invokes multiple API targets:
        // - googleapis.com
        // - maps.googleapis.com
        if (server != null) {
            server.stubFor(request("POST", urlPathEqualTo("/geolocation/v1/geolocate"))
                    .withQueryParam("key", matching(".*"))
                    .withRequestBody(equalToJson("{\"considerIp\": true}"))
                    .willReturn(aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"location\":{\"lat\":24.7768404,\"lng\":-76.2849047},\"accuracy\":8252}")));
        }

        RestAssured.get()
                .then()
                .statusCode(200)
                .body("[0]", hasKey("addressComponents"));
    }

    @Test
    public void loadAddress() {
        RestAssured.get("/address/calle marie curie, sevilla, sevilla")
                .then()
                .statusCode(200)
                .body("[0]", hasKey("addressComponents"));
    }

    @Test
    public void loadLatLong() {
        RestAssured.get("/lat/37.8021028/lon/-122.41875")
                .then()
                .statusCode(200)
                .body("[0]", hasKey("addressComponents"));
    }

}
