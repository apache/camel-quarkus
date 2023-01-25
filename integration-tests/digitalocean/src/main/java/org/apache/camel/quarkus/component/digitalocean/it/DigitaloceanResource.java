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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.myjeeva.digitalocean.pojo.Account;
import com.myjeeva.digitalocean.pojo.Action;
import com.myjeeva.digitalocean.pojo.Backup;
import com.myjeeva.digitalocean.pojo.Delete;
import com.myjeeva.digitalocean.pojo.Droplet;
import com.myjeeva.digitalocean.pojo.FloatingIP;
import com.myjeeva.digitalocean.pojo.Image;
import com.myjeeva.digitalocean.pojo.Key;
import com.myjeeva.digitalocean.pojo.Region;
import com.myjeeva.digitalocean.pojo.Size;
import com.myjeeva.digitalocean.pojo.Snapshot;
import com.myjeeva.digitalocean.pojo.Tag;
import com.myjeeva.digitalocean.pojo.Volume;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.digitalocean.constants.DigitalOceanHeaders;
import org.apache.camel.component.digitalocean.constants.DigitalOceanOperations;
import org.jboss.logging.Logger;

@SuppressWarnings("unchecked")
@Path("digitalocean")
@ApplicationScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class DigitaloceanResource {

    private static final Logger LOG = Logger.getLogger(DigitaloceanResource.class);
    private static final String REGION_FRANKFURT = "fra1";
    private static final String DROPLET_SIZE_1_GB = "s-1vcpu-1gb";
    private static final String DROPLET_IMAGE = "ubuntu-20-04-x64";
    private static final List<String> DROPLET_TAGS = Arrays.asList("tag1", "tag2");
    private static final int VOLUME_SIZE_5_GB = 5;

    @Inject
    ProducerTemplate producerTemplate;

    @PUT
    @Path("droplet/{name}")
    public int createDroplet(@PathParam("name") String name) {
        LOG.infof("creating a droplet with name %s", name);
        Map<String, Object> headers = createHeaders(name);
        Droplet droplet = producerTemplate.requestBodyAndHeaders("direct:droplet", null, headers, Droplet.class);
        return droplet.getId();
    }

    /**
     * Creates the headers for operation `Create a Droplet`
     */
    private Map<String, Object> createHeaders(String name) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(DigitalOceanHeaders.OPERATION, DigitalOceanOperations.create);
        headers.put(DigitalOceanHeaders.REGION, REGION_FRANKFURT);
        headers.put(DigitalOceanHeaders.DROPLET_SIZE, DROPLET_SIZE_1_GB);
        headers.put(DigitalOceanHeaders.DROPLET_IMAGE, DROPLET_IMAGE);
        headers.put(DigitalOceanHeaders.DROPLET_TAGS, DROPLET_TAGS);
        headers.put(DigitalOceanHeaders.NAME, name);
        return headers;
    }

    @DELETE
    @Path("droplet/{id}")
    public Response deleteDroplet(@PathParam("id") int id) {
        LOG.infof("deleting a droplet with id %s", id);
        Map<String, Object> headers = new HashMap<>();
        headers.put(DigitalOceanHeaders.OPERATION, DigitalOceanOperations.delete);
        headers.put(DigitalOceanHeaders.ID, id);
        producerTemplate.sendBodyAndHeaders("direct:droplet", null, headers);
        return Response.accepted().build();
    }

    @GET
    @Path("droplet/{id}")
    public Droplet getDroplet(@PathParam("id") int id) {
        LOG.infof("getting droplet with id %s", id);
        Map<String, Object> headers = new HashMap<>();
        headers.put(DigitalOceanHeaders.OPERATION, DigitalOceanOperations.get);
        headers.put(DigitalOceanHeaders.ID, id);
        return producerTemplate.requestBodyAndHeaders("direct:droplet", null, headers, Droplet.class);
    }

    @GET
    @Path("droplet/actions/{id}")
    public List<Action> getActions(@PathParam("id") int id) {
        LOG.infof("getting action's droplet with id %s", id);
        Map<String, Object> headers = new HashMap<>();
        headers.put(DigitalOceanHeaders.OPERATION, DigitalOceanOperations.listActions);
        headers.put(DigitalOceanHeaders.ID, id);
        return producerTemplate.requestBodyAndHeaders("direct:droplet", null, headers, List.class);
    }

    @POST
    @Path("droplet/snapshot/{id}")
    public Action snapshotDroplet(@PathParam("id") int id, String snapshotName) {
        LOG.infof("snapshot droplet %s with name %s", id, snapshotName);
        Map<String, Object> headers = new HashMap<>();
        headers.put(DigitalOceanHeaders.OPERATION, DigitalOceanOperations.takeSnapshot);
        headers.put(DigitalOceanHeaders.ID, id);
        headers.put(DigitalOceanHeaders.NAME, snapshotName);
        return producerTemplate.requestBodyAndHeaders("direct:droplet", null, headers, Action.class);
    }

    @GET
    @Path("droplet/snapshots/{id}")
    public List<Snapshot> getSnapshots(@PathParam("id") int id) {
        LOG.infof("getting snapshot's droplet with id %s", id);
        Map<String, Object> headers = new HashMap<>();
        headers.put(DigitalOceanHeaders.OPERATION, DigitalOceanOperations.listSnapshots);
        headers.put(DigitalOceanHeaders.ID, id);
        return producerTemplate.requestBodyAndHeaders("direct:droplet", null, headers, List.class);
    }

    @GET
    @Path("droplet/backups/enable/{id}")
    public Action enableBackups(@PathParam("id") int id) {
        LOG.infof("Enable backups for droplet %s", id);
        return doAction(id, DigitalOceanOperations.enableBackups);
    }

    @GET
    @Path("droplet/backups/disable/{id}")
    public Action disableBackups(@PathParam("id") int id) {
        LOG.infof("disable backups for droplet %s", id);
        return doAction(id, DigitalOceanOperations.disableBackups);
    }

    @GET
    @Path("droplet/on/{id}")
    public Action turnOn(@PathParam("id") int id) {
        LOG.infof("Turn on droplet %s", id);
        return doAction(id, DigitalOceanOperations.powerOn);
    }

    @GET
    @Path("droplet/off/{id}")
    public Action turnOff(@PathParam("id") int id) {
        LOG.infof("Turn off droplet %s", id);
        return doAction(id, DigitalOceanOperations.powerOff);
    }

    @GET
    @Path("droplet/reboot/{id}")
    public Action rebootDroplet(@PathParam("id") int id) {
        LOG.infof("Reboot droplet %s", id);
        return doAction(id, DigitalOceanOperations.reboot);
    }

    @GET
    @Path("droplet/ipv6/{id}")
    public Action enableIpv6(@PathParam("id") int id) {
        LOG.infof("Enable Ipv6 for droplet %s", id);
        return doAction(id, DigitalOceanOperations.enableIpv6);
    }

    private Action doAction(int id, DigitalOceanOperations operation) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(DigitalOceanHeaders.OPERATION, operation);
        headers.put(DigitalOceanHeaders.ID, id);
        return producerTemplate.requestBodyAndHeaders("direct:droplet", null, headers, Action.class);
    }

    @GET
    @Path("droplet/backups/{id}")
    public List<Backup> getBackups(@PathParam("id") int id) {
        LOG.infof("getting backup's droplet with id %s", id);
        Map<String, Object> headers = new HashMap<>();
        headers.put(DigitalOceanHeaders.OPERATION, DigitalOceanOperations.listBackups);
        headers.put(DigitalOceanHeaders.ID, id);
        return producerTemplate.requestBodyAndHeaders("direct:droplet", null, headers, List.class);
    }

    @GET
    @Path("droplet/neighbors/{id}")
    public List<Droplet> getNeighbors(@PathParam("id") int id) {
        LOG.infof("getting neighbor's droplet with id %s", id);
        Map<String, Object> headers = new HashMap<>();
        headers.put(DigitalOceanHeaders.OPERATION, DigitalOceanOperations.listNeighbors);
        headers.put(DigitalOceanHeaders.ID, id);
        return producerTemplate.requestBodyAndHeaders("direct:droplet", null, headers, List.class);
    }

    @GET
    @Path("droplet/neighbors")
    public List<Droplet> getAllNeighbors() {
        LOG.infof("getting all neighbors");
        Map<String, Object> headers = new HashMap<>();
        headers.put(DigitalOceanHeaders.OPERATION, DigitalOceanOperations.listAllNeighbors);
        return producerTemplate.requestBodyAndHeaders("direct:droplet", null, headers, List.class);
    }

    @GET
    @Path("account")
    public Account getAccount() {
        LOG.infof("getting the account");
        return producerTemplate.requestBody("direct:account", null, Account.class);
    }

    @GET
    @Path("action/{id}")
    public Action getAction(@PathParam("id") Integer id) {
        LOG.infof("getting the action %s", id);
        Map<String, Object> headers = new HashMap<>();
        headers.put(DigitalOceanHeaders.ID, id);
        headers.put(DigitalOceanHeaders.OPERATION, DigitalOceanOperations.get);
        return producerTemplate.requestBodyAndHeaders("direct:actions", null, headers, Action.class);
    }

    @GET
    @Path("actions")
    public List<Action> getActions() {
        LOG.infof("getting all account's actions");
        return producerTemplate.requestBodyAndHeader("direct:actions", null, DigitalOceanHeaders.OPERATION,
                DigitalOceanOperations.list, List.class);
    }

    @GET
    @Path("images")
    public List<Image> getImages() {
        LOG.infof("getting all account's images");
        return producerTemplate.requestBodyAndHeader("direct:images", null, DigitalOceanHeaders.OPERATION,
                DigitalOceanOperations.list, List.class);
    }

    @GET
    @Path("images/{id}")
    public Image getImageById(@PathParam("id") Integer id) {
        LOG.infof("getting an image by id %s", id);
        Map<String, Object> headers = new HashMap<>();
        headers.put(DigitalOceanHeaders.ID, id);
        headers.put(DigitalOceanHeaders.OPERATION, DigitalOceanOperations.get);
        return producerTemplate.requestBodyAndHeaders("direct:images", null, headers, Image.class);
    }

    @GET
    @Path("images/user")
    public List<Image> getUserImages() {
        LOG.infof("getting user's images");
        return producerTemplate.requestBodyAndHeader("direct:images", null, DigitalOceanHeaders.OPERATION,
                DigitalOceanOperations.ownList, List.class);
    }

    @GET
    @Path("snapshots")
    public List<Snapshot> getSnapshots() {
        LOG.infof("getting all account's snapshots");
        return producerTemplate.requestBodyAndHeader("direct:snapshots", null, DigitalOceanHeaders.OPERATION,
                DigitalOceanOperations.list, List.class);
    }

    @GET
    @Path("snapshots/{id}")
    public Snapshot getSnapshotById(@PathParam("id") Integer id) {
        LOG.infof("getting an snapshot by id %s", id);
        Map<String, Object> headers = new HashMap<>();
        headers.put(DigitalOceanHeaders.ID, id);
        headers.put(DigitalOceanHeaders.OPERATION, DigitalOceanOperations.get);
        return producerTemplate.requestBodyAndHeaders("direct:snapshots", null, headers, Snapshot.class);
    }

    @DELETE
    @Path("snapshots/{id}")
    public Response deleteSnapshot(@PathParam("id") Integer id) {
        LOG.infof("delete snapshot %s", id);
        Map<String, Object> headers = new HashMap<>();
        headers.put(DigitalOceanHeaders.OPERATION, DigitalOceanOperations.delete);
        headers.put(DigitalOceanHeaders.ID, id);
        producerTemplate.sendBodyAndHeaders("direct:snapshots", null, headers);
        return Response.ok().build();
    }

    @GET
    @Path("sizes")
    public List<Size> getSizes() {
        LOG.infof("getting all available sizes");
        return producerTemplate.requestBody("direct:sizes", null, List.class);
    }

    @GET
    @Path("regions")
    public List<Region> getRegions() {
        LOG.infof("getting all available regions");
        return producerTemplate.requestBody("direct:regions", null, List.class);
    }

    @PUT
    @Path("floatingIP")
    public String createFloatingIP(Integer dropletId) {
        LOG.infof("create floating IP for droplet %s", dropletId);
        Map<String, Object> headers = new HashMap<>();
        headers.put(DigitalOceanHeaders.OPERATION, DigitalOceanOperations.create);
        headers.put(DigitalOceanHeaders.DROPLET_ID, dropletId);
        // if pending task on droplet : API returns an exception. There should be a retry later
        try {
            FloatingIP floatingIP = producerTemplate.requestBodyAndHeaders("direct:floatingIPs", null, headers,
                    FloatingIP.class);
            return floatingIP.getIp();
        } catch (Exception e) {
            LOG.errorf("Enable to create and assign Floating IP - Please retry - error message : %s" + e.getMessage());
        }
        return "";
    }

    @GET
    @Path("floatingIP/{id}")
    public FloatingIP getFloatingIpById(@PathParam("id") String id) {
        LOG.infof("get floating IP %s", id);
        Map<String, Object> headers = new HashMap<>();
        headers.put(DigitalOceanHeaders.OPERATION, DigitalOceanOperations.get);
        headers.put(DigitalOceanHeaders.FLOATING_IP_ADDRESS, id);
        return producerTemplate.requestBodyAndHeaders("direct:floatingIPs", null, headers, FloatingIP.class);
    }

    @GET
    @Path("floatingIP")
    public List<FloatingIP> getAllFloatingIps() {
        LOG.infof("get all floating IPs");
        return producerTemplate.requestBodyAndHeader("direct:floatingIPs", null,
                DigitalOceanHeaders.OPERATION, DigitalOceanOperations.list, List.class);
    }

    @GET
    @Path("floatingIP/unassign/{id}")
    public Action unassignFloatingIp(@PathParam("id") String id) {
        LOG.infof("unassign floating IP %s");
        Map<String, Object> headers = new HashMap<>();
        headers.put(DigitalOceanHeaders.OPERATION, DigitalOceanOperations.unassign);
        headers.put(DigitalOceanHeaders.FLOATING_IP_ADDRESS, id);
        // if pending task on droplet : API returns an exception. There should be a retry later
        try {
            return producerTemplate.requestBodyAndHeaders("direct:floatingIPs", null, headers, Action.class);
        } catch (Exception e) {
            LOG.errorf("Enable to unassign Floating IP - Please retry - error message : %s" + e.getMessage());
        }
        return new Action();
    }

    @DELETE
    @Path("floatingIP/{id}")
    public Delete deleteFloatingIp(@PathParam("id") String id) {
        LOG.infof("delete floating IP %s", id);
        Map<String, Object> headers = new HashMap<>();
        headers.put(DigitalOceanHeaders.OPERATION, DigitalOceanOperations.delete);
        headers.put(DigitalOceanHeaders.FLOATING_IP_ADDRESS, id);
        // if pending task on droplet : API returns an exception. There should be a retry later
        try {
            return producerTemplate.requestBodyAndHeaders("direct:floatingIPs", null, headers, Delete.class);
        } catch (Exception e) {
            LOG.errorf("Enable to delete Floating IP - Please retry - error message : %s" + e.getMessage());
        }
        return new Delete(false);

    }

    @GET
    @Path("floatingIP/actions/{id}")
    public List<Action> getFloatingIpsActions(@PathParam("id") String id) {
        LOG.infof("get all floating IPs actions %s", id);
        Map<String, Object> headers = new HashMap<>();
        headers.put(DigitalOceanHeaders.OPERATION, DigitalOceanOperations.listActions);
        headers.put(DigitalOceanHeaders.FLOATING_IP_ADDRESS, id);
        return (List<Action>) producerTemplate.requestBodyAndHeaders("direct:floatingIPs", null, headers, List.class);
    }

    @PUT
    @Path("blockStorages")
    public String createVolume(String name) {
        LOG.infof("create volume for region %s with size %s named %s", REGION_FRANKFURT, VOLUME_SIZE_5_GB, name);
        Map<String, Object> headers = new HashMap<>();
        headers.put(DigitalOceanHeaders.OPERATION, DigitalOceanOperations.create);
        headers.put(DigitalOceanHeaders.REGION, REGION_FRANKFURT);
        headers.put(DigitalOceanHeaders.VOLUME_SIZE_GIGABYTES, VOLUME_SIZE_5_GB);
        headers.put(DigitalOceanHeaders.NAME, name);
        headers.put(DigitalOceanHeaders.DESCRIPTION, "volume_" + name);
        Volume volume = producerTemplate.requestBodyAndHeaders("direct:blockStorages", null, headers, Volume.class);
        return volume.getId();
    }

    @GET
    @Path("blockStorages/{id}")
    public Volume getVolumeById(@PathParam("id") String volumeId) {
        LOG.infof("getting volume %s", volumeId);
        Map<String, Object> headers = new HashMap<>();
        headers.put(DigitalOceanHeaders.OPERATION, DigitalOceanOperations.get);
        headers.put(DigitalOceanHeaders.ID, volumeId);
        headers.put(DigitalOceanHeaders.REGION, REGION_FRANKFURT);
        return producerTemplate.requestBodyAndHeaders("direct:blockStorages", null, headers, Volume.class);
    }

    @POST
    @Path("blockStorages/attach/{name}")
    public Action attachVolumeToDroplet(@PathParam("name") String volumeName, String dropletId) {
        LOG.infof("attach volume %s to droplet %s in region %s", volumeName, dropletId, REGION_FRANKFURT);
        Map<String, Object> headers = new HashMap<>();
        headers.put(DigitalOceanHeaders.OPERATION, DigitalOceanOperations.attach);
        headers.put(DigitalOceanHeaders.VOLUME_NAME, volumeName);
        headers.put(DigitalOceanHeaders.DROPLET_ID, dropletId);
        headers.put(DigitalOceanHeaders.REGION, REGION_FRANKFURT);
        return producerTemplate.requestBodyAndHeaders("direct:blockStorages", null, headers, Action.class);
    }

    @POST
    @Path("blockStorages/detach/{name}")
    public Action detachVolumeToDroplet(@PathParam("name") String volumeName, String dropletId) {
        LOG.infof("detach volume %s to droplet %s in region %s", volumeName, dropletId, REGION_FRANKFURT);
        Map<String, Object> headers = new HashMap<>();
        headers.put(DigitalOceanHeaders.OPERATION, DigitalOceanOperations.detach);
        headers.put(DigitalOceanHeaders.VOLUME_NAME, volumeName);
        headers.put(DigitalOceanHeaders.DROPLET_ID, dropletId);
        headers.put(DigitalOceanHeaders.REGION, REGION_FRANKFURT);
        return producerTemplate.requestBodyAndHeaders("direct:blockStorages", null, headers, Action.class);
    }

    @GET
    @Path("blockStorages")
    public List<Volume> getAvailableVolumes() {
        LOG.infof("get all avalaible volumes for region %s", REGION_FRANKFURT);
        Map<String, Object> headers = new HashMap<>();
        headers.put(DigitalOceanHeaders.OPERATION, DigitalOceanOperations.list);
        headers.put(DigitalOceanHeaders.REGION, REGION_FRANKFURT);
        return producerTemplate.requestBodyAndHeaders("direct:blockStorages", null, headers, List.class);
    }

    @DELETE
    @Path("blockStorages")
    public Delete deleteVolume(String volumeId) {
        LOG.infof("delete volume %s", volumeId);
        Map<String, Object> headers = new HashMap<>();
        headers.put(DigitalOceanHeaders.OPERATION, DigitalOceanOperations.delete);
        headers.put(DigitalOceanHeaders.ID, volumeId);
        return producerTemplate.requestBodyAndHeaders("direct:blockStorages", null, headers, Delete.class);
    }

    @PUT
    @Path("keys/{name}")
    public Integer createKey(@PathParam("name") String name, String publicKey) {
        LOG.infof("create key %s", name);
        Map<String, Object> headers = new HashMap<>();
        headers.put(DigitalOceanHeaders.OPERATION, DigitalOceanOperations.create);
        headers.put(DigitalOceanHeaders.NAME, name);
        headers.put(DigitalOceanHeaders.KEY_PUBLIC_KEY, publicKey);
        Key key = producerTemplate.requestBodyAndHeaders("direct:keys", null, headers, Key.class);
        return key.getId();
    }

    @GET
    @Path("keys/{id}")
    public Key getKeyById(@PathParam("id") Integer id) {
        LOG.infof("get key %s", id);
        Map<String, Object> headers = new HashMap<>();
        headers.put(DigitalOceanHeaders.OPERATION, DigitalOceanOperations.get);
        headers.put(DigitalOceanHeaders.ID, id);
        return producerTemplate.requestBodyAndHeaders("direct:keys", null, headers, Key.class);
    }

    @GET
    @Path("keys")
    public List<Key> getKeys() {
        LOG.infof("get keys");
        return producerTemplate.requestBodyAndHeader("direct:keys", null, DigitalOceanHeaders.OPERATION,
                DigitalOceanOperations.list, List.class);
    }

    @POST
    @Path("keys/{id}")
    public Key updateKey(@PathParam("id") Integer id, String name) {
        LOG.infof("update key %s with name %s", id, name);
        Map<String, Object> headers = new HashMap<>();
        headers.put(DigitalOceanHeaders.OPERATION, DigitalOceanOperations.update);
        headers.put(DigitalOceanHeaders.ID, id);
        headers.put(DigitalOceanHeaders.NAME, name);
        return producerTemplate.requestBodyAndHeaders("direct:keys", null, headers, Key.class);
    }

    @DELETE
    @Path("keys/{id}")
    public Delete deleteKey(@PathParam("id") Integer id) {
        LOG.infof("delete key %s", id);
        Map<String, Object> headers = new HashMap<>();
        headers.put(DigitalOceanHeaders.OPERATION, DigitalOceanOperations.delete);
        headers.put(DigitalOceanHeaders.ID, id);
        return producerTemplate.requestBodyAndHeaders("direct:keys", null, headers, Delete.class);
    }

    @POST
    @Path("tags/{name}")
    public Tag createTag(@PathParam("name") String name) {
        LOG.infof("create tag %s", name);
        Map<String, Object> headers = new HashMap<>();
        headers.put(DigitalOceanHeaders.OPERATION, DigitalOceanOperations.create);
        headers.put(DigitalOceanHeaders.NAME, name);
        return producerTemplate.requestBodyAndHeaders("direct:tags", null, headers, Tag.class);
    }

    @GET
    @Path("tags/{name}")
    public Tag getTag(@PathParam("name") String name) {
        LOG.infof("get tag %s", name);
        Map<String, Object> headers = new HashMap<>();
        headers.put(DigitalOceanHeaders.OPERATION, DigitalOceanOperations.get);
        headers.put(DigitalOceanHeaders.NAME, name);
        return producerTemplate.requestBodyAndHeaders("direct:tags", null, headers, Tag.class);
    }

    @GET
    @Path("tags")
    public List<Tag> getAllTags() {
        LOG.infof("get tags");
        return producerTemplate.requestBodyAndHeader("direct:tags", null, DigitalOceanHeaders.OPERATION,
                DigitalOceanOperations.list, List.class);
    }

    @DELETE
    @Path("tags/{name}")
    public Delete deleteTag(@PathParam("name") String name) {
        LOG.infof("delete tag %s", name);
        Map<String, Object> headers = new HashMap<>();
        headers.put(DigitalOceanHeaders.OPERATION, DigitalOceanOperations.delete);
        headers.put(DigitalOceanHeaders.NAME, name);
        return producerTemplate.requestBodyAndHeaders("direct:tags", null, headers, Delete.class);
    }
}
