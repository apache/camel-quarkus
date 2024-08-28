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

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@TestHTTPEndpoint(GeocoderNominationResource.class)
@QuarkusTestResource(GeocoderTestResource.class)
public class GeocoderNominationTest {

    /**
     * Note: The geocoder:nominatim endpoints call out to an API that is currently not covered by WireMock.
     *
     * https://issues.apache.org/jira/browse/CAMEL-15900
     * https://github.com/apache/camel-quarkus/issues/2033
     */

    @Test
    public void loadAddress() {
        RestAssured.get("/address/calle marie curie, sevilla, sevilla")
                .then()
                .statusCode(200)
                .body("status", equalTo("OK"))
                .body("postalCode", notNullValue())
                .body("region.name", equalTo("Andalucía"))
                .body("country.shortCode", equalTo("ES"));
    }

    @Test
    public void loadLatLong() {
        RestAssured.get("/lat/37.8021028/lon/-122.41875")
                .then()
                .statusCode(200)
                .body("status", equalTo("OK"))
                .body("postalCode", equalTo("90214"))
                .body("city", equalTo("San Francisco"))
                .body("country.shortCode", equalTo("US"));
    }
}
