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
package org.apache.camel.quarkus.component.digitalocean.it;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.quarkus.test.wiremock.MockServer;
import org.eclipse.microprofile.config.ConfigProvider;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeAll;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("unchecked")
@QuarkusTest
@QuarkusTestResource(DigitaloceanTestResource.class)
class DigitaloceanTest {
    static String publicKey;
    @MockServer
    WireMockServer server;

    @BeforeAll
    public static void initPublicKey() {
        publicKey = ConfigProvider.getConfig().getOptionalValue("DIGITALOCEAN_PUBLIC_KEY", String.class).orElse(
                "ssh-rsa AEXAMPLEaC1yc2EAAAADAQABAAAAQQDDHr/jh2Jy4yALcK4JyWbVkPRaWmhck3IgCoeOO3z1e2dBowLh64QAM+Qb72pxekALga2oi4GvT+TlWNhzPH4V example");
    }

    //@Test
    void testAccount() {
        given()
                .when()
                .get("/digitalocean/account/")
                .then()
                .body("uuid", notNullValue());
    }

    //@Test
    void testSizes() {
        List<Map<String, Object>> sizes = RestAssured.given().contentType(ContentType.JSON)
                .get("/digitalocean/sizes")
                .then().extract().body().as(List.class);
        assertNotNull(sizes);
        Optional<Map<String, Object>> size_1Gb = sizes.stream()
                .filter(s -> "s-1vcpu-1gb".equals(s.get("slug")))
                .findFirst();
        assertTrue(size_1Gb.isPresent());
    }

    //@Test
    void testRegions() {
        List<Map<String, Object>> regions = RestAssured.given().contentType(ContentType.JSON)
                .get("/digitalocean/regions")
                .then().extract().body().as(List.class);
        assertNotNull(regions);
        Optional<Map<String, Object>> nyc1Region = regions.stream()
                .filter(r -> "nyc1".equals(r.get("slug")))
                .findFirst();
        assertTrue(nyc1Region.isPresent());
    }

    //@Test
    void testKeys() {
        // create a key
        String name = "TestKey1";

        Integer publicKeyId = given()
                .contentType(ContentType.JSON)
                .body(publicKey)
                .put("/digitalocean/keys/" + name)
                .then().extract().body().as(Integer.class);

        // get the key
        given()
                .contentType(ContentType.JSON)
                .get("/digitalocean/keys/" + publicKeyId)
                .then()
                .body("id", equalTo(publicKeyId))
                .body("name", equalTo(name));

        // update key
        name = "updated_TestKey1";
        given()
                .contentType(ContentType.JSON)
                .body(name)
                .post("/digitalocean/keys/" + publicKeyId)
                .then()
                .body("id", equalTo(publicKeyId))
                .body("name", equalTo(name));

        // List keys
        List<Map<String, Object>> keys = given()
                .get("/digitalocean/keys")
                .then().extract().body().as(List.class);
        Optional<Map<String, Object>> resultKey = keys.stream()
                .filter(m -> publicKeyId.equals(m.get("id")))
                .findAny();
        assertTrue(resultKey.isPresent());

        // delete the key
        given()
                .delete("/digitalocean/keys/" + publicKeyId)
                .then()
                .body("isRequestSuccess", CoreMatchers.equalTo(true));

    }

    //@Test
    void testTags() {
        // create a tag
        String name = "awesome";
        given()
                .contentType(ContentType.JSON)
                .post("/digitalocean/tags/" + name)
                .then()
                .body("name", equalTo(name));

        // get a tag
        given()
                .get("/digitalocean/tags/" + name)
                .then()
                .body("name", equalTo(name));

        // get all tags
        List<Map<String, Object>> tags = given()
                .contentType(ContentType.JSON)
                .get("/digitalocean/tags/")
                .then().extract().body().as(List.class);
        Optional<Map<String, Object>> resultKey = tags.stream()
                .filter(t -> name.equals(t.get("name")))
                .findAny();
        assertTrue(resultKey.isPresent());

        // delete the tag
        given()
                .delete("/digitalocean/tags/" + name)
                .then()
                .body("isRequestSuccess", CoreMatchers.equalTo(true));

    }
}
