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
import org.openstack4j.model.identity.v3.Domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Path("/openstack/keystone/domains/")
@ApplicationScoped
public class OpenstackKeystoneDomainResource {

    private static final Logger LOG = Logger.getLogger(OpenstackKeystoneDomainResource.class);

    private static final String URI_FORMAT = "openstack-keystone://{{camel.openstack.test.host-url}}?username=user&password=secret&project=project&operation=%s&subsystem="
            + KeystoneConstants.DOMAINS;

    private static final String DOMAIN_NAME = "Domain_CRUD";
    private static final String DOMAIN_ID = "98c110ae41c249189c9d5c25d8377b65";
    private static final String DOMAIN_DESCRIPTION = "Domain used for CRUD tests";
    private static final String DOMAIN_DESCRIPTION_UPDATED = "An updated domain used for CRUD tests";

    @Inject
    ProducerTemplate template;

    @Path("/createShouldSucceed")
    @POST
    public void createShouldSucceed() {
        LOG.debug("Calling OpenstackKeystoneDomainResource.createShouldSucceed()");

        Domain in = Builders.domain().name(DOMAIN_NAME).description(DOMAIN_DESCRIPTION).enabled(true).build();

        String uri = String.format(URI_FORMAT, OpenstackConstants.CREATE);
        Domain out = template.requestBody(uri, in, Domain.class);

        assertNotNull(out);
        assertEquals(DOMAIN_NAME, out.getName());
        assertEquals(DOMAIN_ID, out.getId());
        assertEquals(DOMAIN_DESCRIPTION, out.getDescription());
    }

    @Path("/getShouldSucceed")
    @POST
    public void getShouldSucceed() {
        LOG.debug("Calling OpenstackKeystoneDomainResource.getShouldSucceed()");

        String uri = String.format(URI_FORMAT, OpenstackConstants.GET);
        Domain out = template.requestBodyAndHeader(uri, null, OpenstackConstants.ID, DOMAIN_ID, Domain.class);

        assertNotNull(out);
        assertEquals(DOMAIN_NAME, out.getName());
        assertEquals(DOMAIN_ID, out.getId());
        assertEquals(DOMAIN_DESCRIPTION, out.getDescription());
        assertFalse(out.isEnabled());
    }

    @Path("/getAllShouldSucceed")
    @POST
    public void getAllShouldSucceed() {
        LOG.debug("Calling OpenstackKeystoneDomainResource.getAllShouldSucceed()");

        String uri = String.format(URI_FORMAT, OpenstackConstants.GET_ALL);
        Domain[] domains = template.requestBody(uri, null, Domain[].class);

        assertEquals(1, domains.length);
        assertEquals("default", domains[0].getId());
        assertNotNull(domains[0].getOptions());
        assertTrue(domains[0].getOptions().isEmpty());
    }

    @Path("/updateShouldSucceed")
    @POST
    public void updateShouldSucceed() {
        LOG.debug("Calling OpenstackKeystoneDomainResource.updateShouldSucceed()");

        Domain in = Builders.domain().name(DOMAIN_NAME).description(DOMAIN_DESCRIPTION_UPDATED).id(DOMAIN_ID).build();

        String uri = String.format(URI_FORMAT, OpenstackConstants.UPDATE);
        Domain out = template.requestBody(uri, in, Domain.class);

        assertNotNull(out);
        assertEquals(DOMAIN_NAME, out.getName());
        assertEquals(DOMAIN_ID, out.getId());
        assertEquals(DOMAIN_DESCRIPTION_UPDATED, out.getDescription());
    }

    @Path("/deleteShouldSucceed")
    @POST
    public void deleteShouldSucceed() {
        LOG.debug("Calling OpenstackKeystoneDomainResource.deleteShouldSucceed()");

        String uri = String.format(URI_FORMAT, OpenstackConstants.DELETE);
        template.requestBodyAndHeader(uri, null, OpenstackConstants.ID, DOMAIN_ID);
    }
}
