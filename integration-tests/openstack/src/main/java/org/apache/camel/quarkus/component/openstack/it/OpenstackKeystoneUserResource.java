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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.openstack.common.OpenstackConstants;
import org.apache.camel.component.openstack.keystone.KeystoneConstants;
import org.jboss.logging.Logger;
import org.openstack4j.api.Builders;
import org.openstack4j.model.identity.v3.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Path("/openstack/keystone/users/")
@ApplicationScoped
public class OpenstackKeystoneUserResource {

    private static final Logger LOG = Logger.getLogger(OpenstackKeystoneUserResource.class);

    private static final String URI_FORMAT = "openstack-keystone://{{camel.openstack.test.host-url}}?username=user&password=secret&project=project&operation=%s&subsystem="
            + KeystoneConstants.USERS;

    private static final String USER_NAME = "foobar";
    private static final String USER_DOMAIN_ID = "default";
    private static final String USER_EMAIL = "foobar@example.org";
    private static final String USER_EMAIL_UPDATED = "updatedFoobar@example.org";
    private static final String USER_ID = "29d5aaaa6d3b471e9c101ae470e649a6";

    @Inject
    ProducerTemplate template;

    @Path("/createShouldSucceed")
    @POST
    public void createShouldSucceed() {
        LOG.debug("Calling OpenstackKeystoneUserResource.createShouldSucceed()");

        User in = Builders.user().name(USER_NAME).domainId(USER_DOMAIN_ID).email(USER_EMAIL).enabled(true).build();

        String uri = String.format(URI_FORMAT, OpenstackConstants.CREATE);
        User out = template.requestBody(uri, in, User.class);

        assertNotNull(out);
        assertEquals(USER_NAME, out.getName());
        assertEquals(USER_DOMAIN_ID, out.getDomainId());
        assertEquals(USER_EMAIL, out.getEmail());
        assertTrue(out.isEnabled());
        assertEquals(USER_ID, out.getId());
    }

    @Path("/getShouldSucceed")
    @POST
    public void getShouldSucceed() {
        LOG.debug("Calling OpenstackKeystoneUserResource.getShouldSucceed()");

        String uri = String.format(URI_FORMAT, OpenstackConstants.GET);
        User out = template.requestBodyAndHeader(uri, null, OpenstackConstants.ID, USER_ID, User.class);

        assertNotNull(out);
        assertEquals(USER_NAME, out.getName());
        assertEquals(USER_DOMAIN_ID, out.getDomainId());
        assertTrue(out.isEnabled());
        assertEquals(USER_ID, out.getId());
    }

    @Path("/getAllShouldSucceed")
    @POST
    public void getAllShouldSucceed() {
        LOG.debug("Calling OpenstackKeystoneUserResource.getAllShouldSucceed()");

        String uri = String.format(URI_FORMAT, OpenstackConstants.GET_ALL);
        User[] users = template.requestBody(uri, null, User[].class);

        assertNotNull(users);
        assertEquals(6, users.length);
        assertEquals("glance", users[0].getName());
        assertEquals("default", users[1].getDomainId());
        assertNull(users[2].getEmail());
        assertEquals("7afec08993c24bb09df141e513738030", users[3].getId());
    }

    @Path("/updateShouldSucceed")
    @POST
    public void updateShouldSucceed() {
        LOG.debug("Calling OpenstackKeystoneUserResource.updateShouldSucceed()");

        User in = Builders.user().id(USER_ID).email(USER_EMAIL_UPDATED).build();

        String uri = String.format(URI_FORMAT, OpenstackConstants.UPDATE);
        User out = template.requestBody(uri, in, User.class);

        assertNotNull(out);
        assertEquals(USER_ID, out.getId());
        assertEquals(USER_EMAIL_UPDATED, out.getEmail());
    }

    @Path("/deleteShouldSucceed")
    @POST
    public void deleteShouldSucceed() {
        LOG.debug("Calling OpenstackKeystoneUserResource.deleteShouldSucceed()");

        String uri = String.format(URI_FORMAT, OpenstackConstants.DELETE);
        template.requestBodyAndHeader(uri, null, OpenstackConstants.ID, USER_ID);
    }
}
