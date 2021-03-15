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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import com.myjeeva.digitalocean.pojo.Action;
import com.myjeeva.digitalocean.pojo.Backup;
import com.myjeeva.digitalocean.pojo.Droplet;
import com.myjeeva.digitalocean.pojo.Kernel;
import com.myjeeva.digitalocean.pojo.Snapshot;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.digitalocean.constants.DigitalOceanHeaders;
import org.apache.camel.component.digitalocean.constants.DigitalOceanOperations;
import org.jboss.logging.Logger;

@Path("digitalocean/droplet")
@ApplicationScoped
public class DigitaloceanResource {

    private static final Logger LOG = Logger.getLogger(DigitaloceanResource.class);
    private static final String REGION_FRANKFURT = "fra1";
    private static final String DROPLET_SIZE_1_GB = "s-1vcpu-1gb";
    private static final String DROPLET_IMAGE = "ubuntu-20-04-x64";

    @Inject
    ProducerTemplate producerTemplate;

    @PUT
    @Path("{name}")
    public int createDroplet(@PathParam("name") String name) {
        LOG.infof("creating a droplet with name %s", name);
        Map<String, Object> headers = getCreateHeaders();
        headers.put(DigitalOceanHeaders.NAME, name);
        Droplet droplet = producerTemplate.requestBodyAndHeaders("direct:droplet", null, headers, Droplet.class);
        return droplet.getId();
    }

    @PUT
    public List<Integer> createDroplet(List<String> names) {
        LOG.infof("creating a droplet with names %s", names);
        Map<String, Object> headers = getCreateHeaders();
        headers.put(DigitalOceanHeaders.NAMES, names);
        List<Droplet> droplets = producerTemplate.requestBodyAndHeaders("direct:droplet", null, headers, List.class);
        return droplets.stream().map(droplet -> droplet.getId()).collect(Collectors.toList());
    }

    private Map<String, Object> getCreateHeaders() {
        Map<String, Object> headers = new HashMap<>();
        headers.put(DigitalOceanHeaders.OPERATION, DigitalOceanOperations.create);
        headers.put(DigitalOceanHeaders.REGION, REGION_FRANKFURT);
        headers.put(DigitalOceanHeaders.DROPLET_SIZE, DROPLET_SIZE_1_GB);
        headers.put(DigitalOceanHeaders.DROPLET_IMAGE, DROPLET_IMAGE);
        Collection<String> tags = new ArrayList<>();
        tags.add("tag1");
        tags.add("tag2");
        headers.put(DigitalOceanHeaders.DROPLET_TAGS, tags);
        return headers;
    }

    @DELETE
    @Path("{id}")
    public Response deleteDroplet(@PathParam("id") int id) {
        LOG.infof("deleting a droplet with id %s", id);
        Map<String, Object> headers = new HashMap<>();
        headers.put(DigitalOceanHeaders.OPERATION, DigitalOceanOperations.delete);
        headers.put(DigitalOceanHeaders.ID, id);
        producerTemplate.sendBodyAndHeaders("direct:droplet", null, headers);
        return Response.accepted().build();
    }

    @GET
    @Path("{id}")
    public Droplet getDroplet(@PathParam("id") int id) {
        LOG.infof("getting droplet with id %s", id);
        Map<String, Object> headers = new HashMap<>();
        headers.put(DigitalOceanHeaders.OPERATION, DigitalOceanOperations.get);
        headers.put(DigitalOceanHeaders.ID, id);
        Droplet droplet = producerTemplate.requestBodyAndHeaders("direct:droplet", null, headers, Droplet.class);
        return droplet;
    }

    @GET
    @Path("actions/{id}")
    public List<Action> getActions(@PathParam("id") int id) {
        LOG.infof("getting actions's droplet with id %s", id);
        Map<String, Object> headers = new HashMap<>();
        headers.put(DigitalOceanHeaders.OPERATION, DigitalOceanOperations.listActions);
        headers.put(DigitalOceanHeaders.ID, id);
        List<Action> actions = producerTemplate.requestBodyAndHeaders("direct:droplet", null, headers, List.class);
        return actions;
    }

    @GET
    @Path("kernels/{id}")
    public List<Kernel> getKernels(@PathParam("id") int id) {
        LOG.infof("getting kernels's droplet with id %s", id);
        Map<String, Object> headers = new HashMap<>();
        headers.put(DigitalOceanHeaders.OPERATION, DigitalOceanOperations.listKernels);
        headers.put(DigitalOceanHeaders.ID, id);
        List<Kernel> kernels = producerTemplate.requestBodyAndHeaders("direct:droplet", null, headers, List.class);
        return kernels;
    }

    @POST
    @Path("snapshot/{id}")
    public Action snapshotDroplet(@PathParam("id") int id, String snapshotName) {
        LOG.infof("snapshot droplet %s with name %s", id, snapshotName);
        Map<String, Object> headers = new HashMap<>();
        headers.put(DigitalOceanHeaders.OPERATION, DigitalOceanOperations.takeSnapshot);
        headers.put(DigitalOceanHeaders.ID, id);
        headers.put(DigitalOceanHeaders.NAME, snapshotName);
        return producerTemplate.requestBodyAndHeaders("direct:droplet", null, headers, Action.class);
    }

    @GET
    @Path("snapshots/{id}")
    public List<Snapshot> getSnapshots(@PathParam("id") int id) {
        LOG.infof("getting snapshot's droplet with id %s", id);
        Map<String, Object> headers = new HashMap<>();
        headers.put(DigitalOceanHeaders.OPERATION, DigitalOceanOperations.listSnapshots);
        headers.put(DigitalOceanHeaders.ID, id);
        List<Snapshot> snapshots = producerTemplate.requestBodyAndHeaders("direct:droplet", null, headers, List.class);
        return snapshots;
    }

    @GET
    @Path("backups/enable/{id}")
    public Action enableBackups(@PathParam("id") int id) {
        LOG.infof("Enable backups for droplet %s", id);
        return doAction(id, DigitalOceanOperations.enableBackups);
    }

    @GET
    @Path("backups/disable/{id}")
    public Action disableBackups(@PathParam("id") int id) {
        LOG.infof("disable backups for droplet %s", id);
        return doAction(id, DigitalOceanOperations.disableBackups);
    }

    @GET
    @Path("on/{id}")
    public Action turnOn(@PathParam("id") int id) {
        LOG.infof("Turn on droplet %s", id);
        return doAction(id, DigitalOceanOperations.powerOn);
    }

    @GET
    @Path("off/{id}")
    public Action turnOff(@PathParam("id") int id) {
        LOG.infof("Turn off droplet %s", id);
        return doAction(id, DigitalOceanOperations.powerOff);
    }

    @GET
    @Path("reboot/{id}")
    public Action rebootDroplet(@PathParam("id") int id) {
        LOG.infof("Reboot droplet %s", id);
        return doAction(id, DigitalOceanOperations.reboot);
    }

    @GET
    @Path("ipv6/{id}")
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
    @Path("backups/{id}")
    public List<Backup> getBackups(@PathParam("id") int id) {
        LOG.infof("getting backups's droplet with id %s", id);
        Map<String, Object> headers = new HashMap<>();
        headers.put(DigitalOceanHeaders.OPERATION, DigitalOceanOperations.listBackups);
        headers.put(DigitalOceanHeaders.ID, id);
        List<Backup> backups = producerTemplate.requestBodyAndHeaders("direct:droplet", null, headers, List.class);
        return backups;
    }

    @GET
    @Path("neighbors/{id}")
    public List<Droplet> getNeighbors(@PathParam("id") int id) {
        LOG.infof("getting neighbors's droplet with id %s", id);
        Map<String, Object> headers = new HashMap<>();
        headers.put(DigitalOceanHeaders.OPERATION, DigitalOceanOperations.listNeighbors);
        headers.put(DigitalOceanHeaders.ID, id);
        List<Droplet> neighbors = producerTemplate.requestBodyAndHeaders("direct:droplet", null, headers, List.class);
        return neighbors;
    }

    @GET
    @Path("neighbors")
    public List<Droplet> getAllNeighbors() {
        LOG.infof("getting all neighbors");
        Map<String, Object> headers = new HashMap<>();
        headers.put(DigitalOceanHeaders.OPERATION, DigitalOceanOperations.listAllNeighbors);
        List<Droplet> neighbors = producerTemplate.requestBodyAndHeaders("direct:droplet", null, headers, List.class);
        return neighbors;
    }

}
