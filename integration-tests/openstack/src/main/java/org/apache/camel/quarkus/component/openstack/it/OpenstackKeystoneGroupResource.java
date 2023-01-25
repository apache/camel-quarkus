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
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.openstack.common.OpenstackConstants;
import org.apache.camel.component.openstack.keystone.KeystoneConstants;
import org.jboss.logging.Logger;
import org.openstack4j.api.Builders;
import org.openstack4j.model.identity.v3.Group;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Path("/openstack/keystone/groups/")
@ApplicationScoped
public class OpenstackKeystoneGroupResource {

    private static final Logger LOG = Logger.getLogger(OpenstackKeystoneGroupResource.class);

    private static final String URI_FORMAT = "openstack-keystone://{{camel.openstack.test.host-url}}?username=user&password=secret&project=project&operation=%s&subsystem="
            + KeystoneConstants.GROUPS;

    private static final String GROUP_NAME = "GROUP_CRUD";
    private static final String GROUP_ID = "c0d675eac29945ad9dfd08aa1bb75751";
    private static final String GROUP_DOMAIN_ID = "default";
    private static final String GROUP_DESCRIPTION = "Group used for CRUD tests";
    private static final String GROUP_DESCRIPTION_UPDATED = "An updated group used for CRUD tests";

    @Inject
    ProducerTemplate template;

    @Path("/createShouldSucceed")
    @POST
    public void createShouldSucceed() {
        LOG.debug("Calling OpenstackKeystoneGroupResource.createShouldSucceed()");

        Group in = Builders.group().name(GROUP_NAME).description(GROUP_DESCRIPTION).domainId(GROUP_DOMAIN_ID).build();

        String uri = String.format(URI_FORMAT, OpenstackConstants.CREATE);
        Group out = template.requestBody(uri, in, Group.class);

        assertNotNull(out);
        assertEquals(GROUP_NAME, out.getName());
        assertEquals(GROUP_DOMAIN_ID, out.getDomainId());
        assertEquals(GROUP_DESCRIPTION, out.getDescription());
    }

    @Path("/getShouldSucceed")
    @POST
    public void getShouldSucceed() {
        LOG.debug("Calling OpenstackKeystoneGroupResource.getShouldSucceed()");

        String uri = String.format(URI_FORMAT, OpenstackConstants.GET);
        Group out = template.requestBodyAndHeader(uri, null, OpenstackConstants.ID, GROUP_ID, Group.class);

        assertNotNull(out);
        assertEquals(GROUP_NAME, out.getName());
        assertEquals(GROUP_ID, out.getId());
        assertEquals(GROUP_DOMAIN_ID, out.getDomainId());
        assertEquals(GROUP_DESCRIPTION, out.getDescription());
    }

    @Path("/getAllShouldSucceed")
    @POST
    public void getAllShouldSucceed() {
        LOG.debug("Calling OpenstackKeystoneGroupResource.getAllShouldSucceed()");

        String uri = String.format(URI_FORMAT, OpenstackConstants.GET_ALL);
        Group[] groups = template.requestBody(uri, null, Group[].class);

        assertEquals(5, groups.length);

        assertEquals("7261c982051c4080a69a52117a861d64", groups[0].getId());
        assertEquals("default", groups[1].getDomainId());
        assertEquals("Group for CRUD tests", groups[2].getDescription());
        assertEquals("role_crud_group", groups[3].getName());
        assertNotNull(groups[4].getLinks());
        assertTrue(groups[4].getLinks().containsKey("self"));
    }

    @Path("/updateShouldSucceed")
    @POST
    public void updateShouldSucceed() {
        LOG.debug("Calling OpenstackKeystoneGroupResource.updateShouldSucceed()");

        Group in = Builders.group().id(GROUP_ID).description(GROUP_DESCRIPTION_UPDATED).build();

        String uri = String.format(URI_FORMAT, OpenstackConstants.UPDATE);
        Group out = template.requestBody(uri, in, Group.class);

        assertNotNull(out);
        assertEquals(GROUP_NAME, out.getName());
        assertEquals(GROUP_ID, out.getId());
        assertEquals(GROUP_DESCRIPTION_UPDATED, out.getDescription());
    }

    @Path("/deleteShouldSucceed")
    @POST
    public void deleteShouldSucceed() {
        LOG.debug("Calling OpenstackKeystoneGroupResource.deleteShouldSucceed()");

        String uri = String.format(URI_FORMAT, OpenstackConstants.DELETE);
        template.requestBodyAndHeader(uri, null, OpenstackConstants.ID, GROUP_ID);
    }

    @Path("/addUserToGroupShouldSucceed")
    @POST
    public void addUserToGroupShouldSucceed() {
        LOG.debug("Calling OpenstackKeystoneGroupResource.addUserToGroupShouldSucceed()");

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeystoneConstants.USER_ID, "d599b83141fc47bc9c25e89267aa27c4");
        headers.put(KeystoneConstants.GROUP_ID, "851398fccda34f208e1839ebbc1251d1");

        String uri = String.format(URI_FORMAT, KeystoneConstants.ADD_USER_TO_GROUP);
        template.requestBodyAndHeaders(uri, null, headers, Group.class);
    }

    @Path("/checkUserGroupShouldSucceed")
    @POST
    public void checkUserGroupShouldSucceed() {
        LOG.debug("Calling OpenstackKeystoneGroupResource.checkUserGroupShouldSucceed()");

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeystoneConstants.USER_ID, "d599b83141fc47bc9c25e89267aa27c4");
        headers.put(KeystoneConstants.GROUP_ID, "851398fccda34f208e1839ebbc1251d1");

        String uri = String.format(URI_FORMAT, KeystoneConstants.CHECK_GROUP_USER);
        boolean userInGroup = template.requestBodyAndHeaders(uri, null, headers, Boolean.class);
        assertTrue(userInGroup);
    }

    @Path("/removeUserFromGroupShouldSucceed")
    @POST
    public void removeUserFromGroupShouldSucceed() {
        LOG.debug("Calling OpenstackKeystoneGroupResource.removeUserFromGroupShouldSucceed()");

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeystoneConstants.USER_ID, "d599b83141fc47bc9c25e89267aa27c4");
        headers.put(KeystoneConstants.GROUP_ID, "851398fccda34f208e1839ebbc1251d1");

        String uri = String.format(URI_FORMAT, KeystoneConstants.REMOVE_USER_FROM_GROUP);
        template.requestBodyAndHeaders(uri, null, headers);
    }
}
