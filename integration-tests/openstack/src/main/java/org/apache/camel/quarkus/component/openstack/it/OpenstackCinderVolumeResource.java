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
package org.apache.camel.quarkus.component.openstack.it;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.openstack.cinder.CinderConstants;
import org.apache.camel.component.openstack.common.OpenstackConstants;
import org.jboss.logging.Logger;
import org.openstack4j.api.Builders;
import org.openstack4j.model.storage.block.Volume;
import org.openstack4j.model.storage.block.VolumeAttachment;
import org.openstack4j.model.storage.block.VolumeType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Path("/openstack/cinder/volumes/")
@ApplicationScoped
public class OpenstackCinderVolumeResource {

    private static final Logger LOG = Logger.getLogger(OpenstackCinderVolumeResource.class);

    private static final String URI_FORMAT = "openstack-cinder://{{camel.openstack.test.host-url}}?username=user&password=secret&project=project&operation=%s&subsystem="
            + CinderConstants.VOLUMES;

    @Inject
    ProducerTemplate template;

    @Path("/createShouldSucceed")
    @POST
    public void createShouldSucceed() {
        LOG.debug("Calling OpenstackCinderVolumeResource.createShouldSucceed()");

        Volume in = Builders.volume().size(10).name("test_openstack4j").description("test").multiattach(true).build();

        String uri = String.format(URI_FORMAT, OpenstackConstants.CREATE);
        Volume out = template.requestBody(uri, in, Volume.class);

        assertEquals(10, out.getSize());
        assertEquals(Boolean.TRUE, out.multiattach());
    }

    @Path("/getShouldSucceed")
    @POST
    public void getShouldSucceed() {
        LOG.debug("Calling OpenstackCinderVolumeResource.getShouldSucceed()");

        String uri = String.format(URI_FORMAT, OpenstackConstants.GET);
        String id = "8a9287b7-4f4d-4213-8d75-63470f19f27c";
        Volume out = template.requestBodyAndHeader(uri, null, CinderConstants.VOLUME_ID, id, Volume.class);

        assertEquals(id, out.getId());
        assertEquals("test-volume", out.getName());
        assertEquals("a description", out.getDescription());
        assertNotNull(out.getCreated());
        assertEquals("nova", out.getZone());
        assertEquals(100, out.getSize());
        assertEquals(Volume.Status.IN_USE, out.getStatus());
        assertEquals("22222222-2222-2222-2222-222222222222", out.getSnapshotId());
        assertEquals("11111111-1111-1111-1111-111111111111", out.getSourceVolid());
        assertEquals("Gold", out.getVolumeType());

        assertNotNull(out.getMetaData());
        Map<String, String> metadata = out.getMetaData();
        assertEquals("False", metadata.get("readonly"));
        assertEquals("rw", metadata.get("attached_mode"));

        assertNotNull(out.getAttachments());
        List<? extends VolumeAttachment> attachments = out.getAttachments();
        assertEquals(1, attachments.size());
        assertEquals("/dev/vdd", attachments.get(0).getDevice());
        assertEquals("myhost", attachments.get(0).getHostname());
        assertEquals("8a9287b7-4f4d-4213-8d75-63470f19f27c", attachments.get(0).getId());
        assertEquals("eaa6a54d-35c1-40ce-831d-bb61f991e1a9", attachments.get(0).getServerId());
        assertEquals("8a9287b7-4f4d-4213-8d75-63470f19f27c", attachments.get(0).getVolumeId());
    }

    @Path("/getAllShouldSucceed")
    @POST
    public void getAllShouldSucceed() {
        LOG.debug("Calling OpenstackCinderVolumeResource.getAllShouldSucceed()");

        String uri = String.format(URI_FORMAT, OpenstackConstants.GET_ALL);
        Volume[] volumes = template.requestBody(uri, null, Volume[].class);

        assertEquals(3, volumes.length);
        assertEquals("b0b5ed7ae06049688349fe43737796d4", volumes[0].getTenantId());
    }

    @Path("/getAllTypesShouldSucceed")
    @POST
    public void getAllTypesShouldSucceed() {
        LOG.debug("Calling OpenstackCinderVolumeResource.getAllTypesShouldSucceed()");

        String uri = String.format(URI_FORMAT, CinderConstants.GET_ALL_TYPES);
        VolumeType[] volumeTypes = template.requestBody(uri, null, VolumeType[].class);

        assertEquals(2, volumeTypes.length);
        assertEquals("6a65bc1b-197b-45bf-8056-9695dc82191f", volumeTypes[0].getId());
        assertEquals("testVolume1", volumeTypes[0].getName());
        assertNotNull(volumeTypes[0].getExtraSpecs());
        assertEquals("gpu", volumeTypes[0].getExtraSpecs().get("capabilities"));
        assertEquals("10f00bb7-46d8-4f3f-b89b-702693a3dcdc", volumeTypes[1].getId());
        assertEquals("testVolume2", volumeTypes[1].getName());
        assertNotNull(volumeTypes[1].getExtraSpecs());
        assertEquals("gpu", volumeTypes[1].getExtraSpecs().get("capabilities"));
    }

    @Path("/updateShouldSucceed")
    @POST
    public void updateShouldSucceed() {
        LOG.debug("Calling OpenstackCinderVolumeResource.updateShouldSucceed()");

        Map<String, Object> headers = new HashMap<>();
        headers.put(CinderConstants.VOLUME_ID, "fffab33e-38e8-4626-9fee-fe90f240ff0f");
        headers.put(OpenstackConstants.NAME, "name");
        headers.put(OpenstackConstants.DESCRIPTION, "description");
        headers.put(CinderConstants.DESCRIPTION, 1024);
        headers.put(CinderConstants.VOLUME_TYPE, "volume-type");
        headers.put(CinderConstants.IMAGE_REF, "image-ref");
        headers.put(CinderConstants.SNAPSHOT_ID, "snaphot-id");
        headers.put(CinderConstants.IS_BOOTABLE, false);

        String uri = String.format(URI_FORMAT, OpenstackConstants.UPDATE);
        template.requestBodyAndHeaders(uri, null, headers);
    }

    @Path("/deleteShouldSucceed")
    @POST
    public void deleteShouldSucceed() {
        LOG.debug("Calling OpenstackCinderVolumeResource.deleteShouldSucceed()");

        String uri = String.format(URI_FORMAT, OpenstackConstants.DELETE);
        template.requestBodyAndHeader(uri, null, CinderConstants.VOLUME_ID, "fffab33e-38e8-4626-9fee-fe90f240ff0f");
    }
}
