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
import java.util.concurrent.atomic.AtomicReference;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.quarkus.test.wiremock.MockServer;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("unchecked")
@QuarkusTest
@QuarkusTestResource(DigitaloceanTestResource.class)
public class DigitaloceanDropletTest {
    static final long timeout = 5;
    static TimeUnit timeoutUnit = TimeUnit.SECONDS;
    static boolean waitBlockStorageAction = false;

    @MockServer
    WireMockServer server;

    @BeforeAll
    public static void initTimeoutUnit() {
        // add timeout if not using MockServer
        // when using a Digitalocean Key, it takes at least 2 minutes to create a droplet or snapshot
        Optional<String> key = ConfigProvider.getConfig().getOptionalValue("DIGITALOCEAN_AUTH_TOKEN", String.class);
        if (key.isPresent()) {
            timeoutUnit = TimeUnit.MINUTES;
            waitBlockStorageAction = true;
        }
    }

    /**
     * Testing droplet producer and all tests of producers related to an existing droplet
     *
     */
    //@Test
    void testDroplets() throws InterruptedException {
        // insert multiple droplets
        Integer dropletId = RestAssured.given().contentType(ContentType.JSON).put("/digitalocean/droplet/droplet1")
                .then().extract().body().as(Integer.class);
        assertNotNull(dropletId);

        // get the droplet by dropletId1 until its status is active and ready
        // it takes time only if using a oAuthToken from Digitalocean
        waitActiveDroplet(dropletId);

        // test actions on droplet
        performActions(dropletId);

        // test actions
        testResultActions(dropletId);

        // test neighbors
        testNeighbors(dropletId);

        // test floating IP
        testFloatingIP(dropletId);

        // test images
        testImages();

        // test snapshots
        testSnapshots();

        // test block storages
        testBlockStorages(dropletId);

        // delete the droplet with id droplet 1
        given()
                .when()
                .delete("/digitalocean/droplet/" + dropletId)
                .then()
                .statusCode(202);
    }

    private void performActions(Integer dropletId) {
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

        waitForSnapshot(dropletId);

        // action : disable backups
        given()
                .when()
                .get("/digitalocean/droplet/backups/disable/" + dropletId)
                .then()
                .body("resourceId", equalTo(dropletId));

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
    }

    /**
     * Test the result of actions performed
     *
     */
    private void testResultActions(Integer dropletId) {
        // getting the droplet actions
        List<Map<String, Object>> actions = RestAssured.given().contentType(ContentType.JSON)
                .get("/digitalocean/droplet/actions/" + dropletId)
                .then().extract().body().as(List.class);
        List<String> types = Arrays.asList("ENABLE_BACKUPS", "DISABLE_BACKUPS", "SNAPSHOT", "POWER_ON", "POWER_OFF", "REBOOT",
                "ENABLE_IPV6");
        assertActions(actions, types);

        // getting the snapshot action id
        Integer snapshotActionId = getSnapshotActionId(actions);

        // getting one action
        given()
                .get("/digitalocean/action/" + snapshotActionId)
                .then()
                .body("id", equalTo(snapshotActionId));

        // getting all actions on the account
        actions = RestAssured.given().contentType(ContentType.JSON)
                .get("/digitalocean/actions/")
                .then().extract().body().as(List.class);
        types = Arrays.asList("ENABLE_BACKUPS", "DISABLE_BACKUPS", "SNAPSHOT", "POWER_ON", "POWER_OFF", "REBOOT");
        assertActions(actions, types);
    }

    /**
     * Gets the snapshots and waits until the snapshot is created in Digitalocean.
     *
     */
    private void waitForSnapshot(Integer dropletId) {
        await().atMost(timeout, timeoutUnit).until(() -> {
            String path = "/digitalocean/droplet/snapshots/" + dropletId;
            List<Map<String, Object>> result = given().when().get(path).then().extract().as(List.class);
            // Look for the snapshot
            Optional<Map<String, Object>> optional = result.stream()
                    .filter(s -> "snapshot1".equals(s.get("name")))
                    .findAny();
            return optional.isPresent();
        });
    }

    /**
     * Gets the droplet and waits until the droplet is Active in Digitalocean.
     *
     */
    private void waitActiveDroplet(Integer dropletId) {
        await().atMost(timeout, timeoutUnit).until(() -> {
            String path = "/digitalocean/droplet/" + dropletId;
            Map<String, Object> droplet = given()
                    .contentType(ContentType.JSON).get(path).then().extract().as(Map.class);
            return droplet != null && dropletId.equals(droplet.get("id")) && "ACTIVE".equals(droplet.get("status"));
        });
    }

    /**
     * Assert all the actions
     *
     */
    private void assertActions(List<Map<String, Object>> actions, List<String> types) {
        // verify there are actions
        assertNotNull(actions);
        // verify there are at least the 7 created actions in the test
        assertTrue(actions.size() >= types.size());
        types.forEach(type -> assertAction(actions, type));
    }

    /**
     * assert a single action
     *
     */
    private void assertAction(List<Map<String, Object>> actions, String actionType) {
        Optional<Map<String, Object>> optional = actions.stream()
                .filter(a -> actionType.equals(a.get("type")))
                .findAny();
        assertTrue(optional.isPresent());
    }

    /**
     * Get action of type SNAPSHOT
     *
     */
    private Integer getSnapshotActionId(List<Map<String, Object>> actions) {
        return actions.stream()
                .filter(a -> "SNAPSHOT".equals(a.get("type")))
                .findAny()
                .map(b -> (Integer) b.get("id"))
                .orElse(null);
    }

    private void testNeighbors(int dropletId) {
        given()
                .when()
                .get("/digitalocean/droplet/neighbors/" + dropletId)
                .then()
                .statusCode(200);
    }

    private void testImages() {
        // getting all images
        List<Map<String, Object>> images = RestAssured.given().contentType(ContentType.JSON)
                .get("/digitalocean/images")
                .then().extract().body().as(List.class);
        // there's at least the image created for the backup
        assertNotNull(images);

        // get one image id
        Integer imageId = images.stream().findAny().map(image -> (Integer) image.get("id")).orElse(null);
        assertNotNull(imageId);

        // get the image with id
        given().contentType(ContentType.JSON)
                .get("/digitalocean/images/" + imageId)
                .then()
                .body("id", equalTo(imageId));

        // test user's images
        images = RestAssured.given().contentType(ContentType.JSON)
                .get("/digitalocean/images/user")
                .then().extract().body().as(List.class);
        assertNotNull(images);
    }

    private void testSnapshots() {
        // getting all snapshots
        List<Map<String, Object>> images = RestAssured.given().contentType(ContentType.JSON)
                .get("/digitalocean/snapshots")
                .then().extract().body().as(List.class);
        // there's at least the image created for the backup
        assertNotNull(images);

        // get one snapshot id
        Integer snapshotId = images.stream().findAny().map(snapshot -> (Integer) snapshot.get("id")).orElse(null);
        assertNotNull(snapshotId);

        // get the snapshot with id
        given().contentType(ContentType.JSON)
                .get("/digitalocean/snapshots/" + snapshotId)
                .then()
                .body("id", equalTo(snapshotId));

        // delete snapshot
        given().contentType(ContentType.JSON)
                .delete("/digitalocean/snapshots/" + snapshotId)
                .then()
                .statusCode(200);
    }

    private void testFloatingIP(Integer dropletId) {
        AtomicReference<String> floatingIp = new AtomicReference<>();
        // create a floating IP for the droplet
        await().atMost(timeout, timeoutUnit).until(() -> {
            floatingIp.set(given().contentType(ContentType.JSON)
                    .body(dropletId)
                    .put("/digitalocean/floatingIP")
                    .then().extract().body().asString());
            return !floatingIp.get().equals("");
        });

        // gets the floating IP
        given()
                .contentType(ContentType.JSON)
                .get("/digitalocean/floatingIP/" + floatingIp)
                .then()
                .body("ip", equalTo(floatingIp.get()));

        // gets all the floating IP
        List<Map<String, Object>> floatingIps = given()
                .contentType(ContentType.JSON)
                .get("/digitalocean/floatingIP")
                .then().extract().body().as(List.class);
        Optional<Map<String, Object>> floatingIpResult = floatingIps.stream()
                .filter(f -> floatingIp.get().equals(f.get("ip")))
                .findAny();
        assertTrue(floatingIpResult.isPresent());

        // unassign a floating IP
        final Map<String, Object>[] action = new Map[1];
        await().atMost(timeout, timeoutUnit).until(() -> {
            action[0] = given()
                    .contentType(ContentType.JSON)
                    .get("/digitalocean/floatingIP/unassign/" + floatingIp)
                    .then().extract().body().as(Map.class);
            return action[0] != null && "UNASSIGN_FLOATING_IP".equals(action[0].get("type"));
        });
        Integer actionId = (Integer) action[0].get("id");

        // get all action of a floating IP
        List<Map<String, Object>> actions = given()
                .contentType(ContentType.JSON)
                .get("/digitalocean/floatingIP/actions/" + floatingIp)
                .then().extract().body().as(List.class);
        assertNotNull(actions);
        Optional<Map<String, Object>> actionUnassign = actions.stream()
                .filter(a -> actionId.equals(a.get("id")))
                .findAny();
        assertTrue(actionUnassign.isPresent());

        // delete a floating ip
        await().atMost(timeout, timeoutUnit).until(() -> {
            Map<String, Object> delete = given()
                    .contentType(ContentType.JSON)
                    .delete("/digitalocean/floatingIP/" + floatingIp)
                    .then().extract().body().as(Map.class);
            return delete != null && (Boolean) delete.get("isRequestSuccess");
        });
    }

    private void testBlockStorages(Integer dropletId) throws InterruptedException {
        // create a volume
        String volumeId = given()
                .contentType(ContentType.JSON)
                .body("volume1")
                .when()
                .put("/digitalocean/blockStorages")
                .then().extract().body().asString();
        assertNotNull(volumeId);

        // get the volume
        given()
                .get("/digitalocean/blockStorages/" + volumeId)
                .then()
                .body("name", equalTo("volume1"));

        // get all volumes in the region FRA1
        List<Map<String, Object>> volumes = given()
                .get("/digitalocean/blockStorages")
                .then()
                .extract().body().as(List.class);
        assertNotNull(volumes);
        Map<String, Object> expectedResult = volumes.stream()
                .filter(v -> "volume1".equals(v.get("name")))
                .findAny().orElse(null);
        assertNotNull(expectedResult);

        // attach to droplet
        given()
                .contentType(ContentType.JSON)
                .body(dropletId)
                .when()
                .post("/digitalocean/blockStorages/attach/volume1")
                .then()
                .body("type", equalTo("ATTACH"));

        // wait until the volume is attached : or else it's impossible to detach
        waitAttachedVolume(dropletId, volumeId);

        // detach from droplet
        given()
                .contentType(ContentType.JSON)
                .body(dropletId)
                .when()
                .post("/digitalocean/blockStorages/detach/volume1")
                .then()
                .statusCode(200);

        // wait until detach action is performed :: only when using digitalocean
        // if the volume is droped before being detached, there's an error in delete action, and it will be impossible to get the drop result
        if (waitBlockStorageAction) {
            Thread.sleep(10000);
        }

        // drop the volume
        given()
                .contentType(ContentType.JSON)
                .body(volumeId)
                .when()
                .delete("/digitalocean/blockStorages")
                .then()
                .body("isRequestSuccess", equalTo(true));

    }

    /**
     * Gets the droplet and waits until the volume is attached to the droplet
     *
     */
    private void waitAttachedVolume(Integer dropletId, String volumeId) {
        await().atMost(timeout, timeoutUnit).until(() -> {
            String path = "/digitalocean/droplet/" + dropletId;
            Map<String, Object> droplet = given()
                    .contentType(ContentType.JSON).get(path).then().extract().as(Map.class);
            return droplet.get("volumeIds") != null && ((List<String>) droplet.get("volumeIds")).contains(volumeId);
        });
    }
}
