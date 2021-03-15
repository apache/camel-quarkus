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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.quarkus.test.wiremock.MockServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(DigitaloceanTestResource.class)
class DigitaloceanTest {

    @MockServer
    WireMockServer server;

    @Test
    @DisabledIfEnvironmentVariable(named = "DIGITALOCEAN_AUTH_TOKEN", matches = ".+")
    public void testIfMock() {
        testDroplets(5, TimeUnit.SECONDS, false);
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "DIGITALOCEAN_AUTH_TOKEN", matches = ".+")
    public void testIfOAuth() {
        testDroplets(10, TimeUnit.MINUTES, true);
    }

    public void testDroplets(long timeout, TimeUnit timeOutUnit, boolean waitPowerOff) {
        // insert multiple droplets
        List<String> names = Arrays.asList("droplet1");
        List<Integer> otherIds = RestAssured.given().contentType(ContentType.JSON).body(names).put("/digitalocean/droplet")
                .then().extract().body().as(List.class);
        assertNotNull(otherIds);
        assertEquals(1, otherIds.size());
        Integer dropletId1 = otherIds.get(0);

        // get the droplet by dropletId1 until its status is active and ready
        // it takes time only if using a oAuthToken from Digitalocean
        waitActiveDroplet(dropletId1, timeout, timeOutUnit);

        // action : enable backups
        given()
                .contentType(ContentType.JSON)
                .get("/digitalocean/droplet/backups/enable/" + dropletId1)
                .then()
                .body("resourceId", equalTo(dropletId1))
                .body("type", equalTo("ENABLE_BACKUPS"));

        // action : power off, before taking a snapshot
        given()
                .contentType(ContentType.JSON)
                .get("/digitalocean/droplet/off/" + dropletId1)
                .then()
                .body("resourceId", equalTo(dropletId1));

        // take a snapshot
        given()
                .contentType(ContentType.JSON)
                .body("snapshot1")
                .post("/digitalocean/droplet/snapshot/" + dropletId1)
                .then()
                .body("resourceId", equalTo(dropletId1));

        // action : get and wait for the snapshot
        waitForSnapshot(dropletId1, timeout, timeOutUnit);

        // action : disable backups
        given()
                .when()
                .get("/digitalocean/droplet/backups/disable/" + dropletId1)
                .then()
                .body("resourceId", equalTo(dropletId1));

        // wait for Droplet to be active
        waitActiveDroplet(dropletId1, timeout, timeOutUnit);

        // action : power on
        given()
                .contentType(ContentType.JSON)
                .get("/digitalocean/droplet/on/" + dropletId1)
                .then()
                .body("resourceId", equalTo(dropletId1));

        // Reboot droplet
        given()
                .contentType(ContentType.JSON)
                .get("/digitalocean/droplet/reboot/" + dropletId1)
                .then()
                .body("resourceId", equalTo(dropletId1));

        // wait for Droplet to be active
        waitActiveDroplet(dropletId1, timeout, timeOutUnit);

        // enable Ipv6
        given()
                .contentType(ContentType.JSON)
                .get("/digitalocean/droplet/ipv6/" + dropletId1)
                .then()
                .body("resourceId", equalTo(dropletId1));

        // getting the droplet actions
        List<Map> actions = RestAssured.given().contentType(ContentType.JSON).body(names)
                .get("/digitalocean/droplet/actions/" + dropletId1)
                .then().extract().body().as(List.class);
        assertActions(actions);

        // delete the droplet
        given()
                .when()
                .delete("/digitalocean/droplet/" + dropletId1)
                .then()
                .statusCode(202);
    }

    /**
     * Gets the snapshots and waits until the snapshot is created in Digitalocean. It may take 2 to 3 minutes. For tests
     * with Wiremock, no need to wait
     *
     * @param dropletId1
     */
    private void waitForSnapshot(Integer dropletId1, long timeout, TimeUnit timeOutUnit) {
        await().atMost(timeout, timeOutUnit).until(() -> {
            String path = "/digitalocean/droplet/snapshots/" + dropletId1;
            List<Map> result = given().when().get(path).then().extract().as(List.class);
            // Look for the snapshot
            Optional optional = result.stream()
                    .filter(s -> "snapshot1".equals(s.get("name")))
                    .findAny();
            return optional.isPresent();
        });
    }

    /**
     * Gets the droplet and waits until the droplet is Active in Digitalocean. It may take 2 to 3 minutes. For tests with
     * Wiremock, the response is always Active
     *
     * @param dropletId1
     */
    private void waitActiveDroplet(Integer dropletId1, long timeout, TimeUnit timeOutUnit) {
        await().atMost(timeout, timeOutUnit).until(() -> {
            String path = "/digitalocean/droplet/" + dropletId1;
            Map droplet = given()
                    .contentType(ContentType.JSON).get(path).then().extract().as(Map.class);
            return droplet != null && dropletId1.equals(droplet.get("id")) && "ACTIVE".equals(droplet.get("status"));
        });
    }

    /**
     * Assert all the actions
     * 
     * @param actions
     */
    private void assertActions(List<Map> actions) {
        // verify there are actions
        assertNotNull(actions);
        // verify there are at least the 7 created actions in the test
        assertTrue(actions.size() >= 7);
        List<String> types = Arrays.asList("ENABLE_BACKUPS", "DISABLE_BACKUPS", "SNAPSHOT", "POWER_ON", "POWER_OFF", "REBOOT",
                "ENABLE_IPV6");
        types.forEach(type -> assertAction(actions, type));
    }

    /**
     * assert a single action
     * 
     * @param actions
     * @param actionType
     */
    private void assertAction(List<Map> actions, String actionType) {
        Optional<Map> optional = actions.stream()
                .filter(a -> actionType.equals(a.get("type")))
                .findAny();
        assertTrue(optional.isPresent());
    }

    @Test
    @DisabledIfEnvironmentVariable(named = "DIGITALOCEAN_AUTH_TOKEN", matches = ".+")
    void testGetKernels() {
        // only for mock back-end server
        given()
                .when()
                .get("/digitalocean/droplet/kernels/3164494")
                .then()
                .body("[0]", hasKey("id"));
    }

    @Test
    @DisabledIfEnvironmentVariable(named = "DIGITALOCEAN_AUTH_TOKEN", matches = ".+")
    void testNeighbors() {
        // only for mock back-end server
        given()
                .when()
                .get("/digitalocean/droplet/neighbors/3164494")
                .then()
                .body("[0].id", equalTo(3164495));
    }
}
