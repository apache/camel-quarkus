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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(DigitaloceanTestResource.class)
class DigitaloceanTest {

    @MockServer
    WireMockServer server;

    static long timeout = 5;
    static TimeUnit timeoutUnit = TimeUnit.SECONDS;

    @BeforeAll
    public static void initTimeoutUnit() {
        // add timeout if not using MockServer
        // when using a Digitalocean Key, it takes at least 2 minutes to create a droplet or snapshot
        String key = System.getenv("DIGITALOCEAN_AUTH_TOKEN");
        if (key != null) {
            timeoutUnit = TimeUnit.MINUTES;
        }
    }

    @Test
    void testDroplets() {
        // insert multiple droplets
        Integer dropletId = RestAssured.given().contentType(ContentType.JSON).put("/digitalocean/droplet/droplet1")
                .then().extract().body().as(Integer.class);
        assertNotNull(dropletId);

        // get the droplet by dropletId1 until its status is active and ready
        // it takes time only if using a oAuthToken from Digitalocean
        waitActiveDroplet(dropletId);

        // action : enable backups
        given()
                .contentType(ContentType.JSON)
                .get("/digitalocean/droplet/backups/enable/" + dropletId)
                .then()
                .body("resourceId", equalTo(dropletId))
                .body("type", equalTo("ENABLE_BACKUPS"));

        // action : power off, before taking a snapshot
        given()
                .contentType(ContentType.JSON)
                .get("/digitalocean/droplet/off/" + dropletId)
                .then()
                .body("resourceId", equalTo(dropletId));

        // take a snapshot
        given()
                .contentType(ContentType.JSON)
                .body("snapshot1")
                .post("/digitalocean/droplet/snapshot/" + dropletId)
                .then()
                .body("resourceId", equalTo(dropletId));

        // action : disable backups
        given()
                .when()
                .get("/digitalocean/droplet/backups/disable/" + dropletId)
                .then()
                .body("resourceId", equalTo(dropletId));

        // wait for Droplet to be active
        waitActiveDroplet(dropletId);

        // action : power on
        given()
                .contentType(ContentType.JSON)
                .get("/digitalocean/droplet/on/" + dropletId)
                .then()
                .body("resourceId", equalTo(dropletId));

        // Reboot droplet
        given()
                .contentType(ContentType.JSON)
                .get("/digitalocean/droplet/reboot/" + dropletId)
                .then()
                .body("resourceId", equalTo(dropletId));

        // enable Ipv6
        given()
                .contentType(ContentType.JSON)
                .get("/digitalocean/droplet/ipv6/" + dropletId)
                .then()
                .body("resourceId", equalTo(dropletId));

        // getting the droplet actions
        List<Map> actions = RestAssured.given().contentType(ContentType.JSON)
                .get("/digitalocean/droplet/actions/" + dropletId)
                .then().extract().body().as(List.class);
        assertActions(actions);

        // test neigbors
        testNeighbors(dropletId);

        // delete the droplet with id droplet 1
        given()
                .when()
                .delete("/digitalocean/droplet/" + dropletId)
                .then()
                .statusCode(202);

    }

    /**
     * Gets the snapshots and waits until the snapshot is created in Digitalocean.
     *
     * @param dropletId
     */
    private void waitForSnapshot(Integer dropletId) {
        await().atMost(this.timeout, this.timeoutUnit).until(() -> {
            String path = "/digitalocean/droplet/snapshots/" + dropletId;
            List<Map> result = given().when().get(path).then().extract().as(List.class);
            // Look for the snapshot
            Optional optional = result.stream()
                    .filter(s -> "snapshot1".equals(s.get("name")))
                    .findAny();
            return optional.isPresent();
        });
    }

    /**
     * Gets the droplet and waits until the droplet is Active in Digitalocean.
     *
     * @param dropletId
     */
    private void waitActiveDroplet(Integer dropletId) {
        await().atMost(this.timeout, this.timeoutUnit).until(() -> {
            String path = "/digitalocean/droplet/" + dropletId;
            Map droplet = given()
                    .contentType(ContentType.JSON).get(path).then().extract().as(Map.class);
            return droplet != null && dropletId.equals(droplet.get("id")) && "ACTIVE".equals(droplet.get("status"));
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

    void testNeighbors(int dropletId) {
        given()
                .when()
                .get("/digitalocean/droplet/neighbors/" + dropletId)
                .then()
                .statusCode(200);
    }
}
