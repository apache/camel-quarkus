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
import org.apache.camel.component.openstack.neutron.NeutronConstants;
import org.jboss.logging.Logger;
import org.openstack4j.api.Builders;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.NetworkType;
import org.openstack4j.model.network.State;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Path("/openstack/neutron/networks/")
@ApplicationScoped
public class OpenstackNeutronNetworkResource {

    private static final Logger LOG = Logger.getLogger(OpenstackNeutronNetworkResource.class);

    private static final String URI_FORMAT = "openstack-neutron://{{camel.openstack.test.host-url}}?username=user&password=secret&project=project&operation=%s&subsystem="
            + NeutronConstants.NEUTRON_NETWORK_SUBSYSTEM;

    private static final String NETWORK_NAME = "net1";
    private static final String NETWORK_ID = "4e8e5957-649f-477b-9e5b-f1f75b21c03c";

    @Inject
    ProducerTemplate template;

    @Path("/createShouldSucceed")
    @POST
    public void createShouldSucceed() {
        LOG.debug("Calling OpenstackNeutronNetworkResource.createShouldSucceed()");

        Network in = Builders.network().name(NETWORK_NAME).isRouterExternal(true).adminStateUp(true).build();

        String uri = String.format(URI_FORMAT, OpenstackConstants.CREATE);
        Network out = template.requestBody(uri, in, Network.class);

        assertEquals(NETWORK_NAME, out.getName());
        assertEquals(State.ACTIVE, out.getStatus());
        assertTrue(out.isRouterExternal());
    }

    @Path("/getShouldSucceed")
    @POST
    public void getShouldSucceed() {
        LOG.debug("Calling OpenstackNeutronNetworkResource.getShouldSucceed()");

        String uri = String.format(URI_FORMAT, OpenstackConstants.GET);
        Network out = template.requestBodyAndHeader(uri, null, OpenstackConstants.ID, NETWORK_ID, Network.class);

        assertNotNull(out);
        assertEquals(NETWORK_NAME, out.getName());
        assertEquals(State.ACTIVE, out.getStatus());
        assertFalse(out.isRouterExternal());
    }

    @Path("/getAllShouldSucceed")
    @POST
    public void getAllShouldSucceed() {
        LOG.debug("Calling OpenstackNeutronNetworkResource.getAllShouldSucceed()");

        String uri = String.format(URI_FORMAT, OpenstackConstants.GET_ALL);
        Network[] networks = template.requestBody(uri, null, Network[].class);

        assertNotNull(networks);
        assertEquals(1, networks.length);

        assertEquals(NETWORK_NAME, networks[0].getName());
        assertNotNull(networks[0].getSubnets());
        assertEquals(1, networks[0].getSubnets().size());
        assertEquals("0c4faf33-8c23-4dc9-8bf5-30dd1ab452f9", networks[0].getSubnets().get(0));
        assertEquals("73f6f1ac-5e58-4801-88c3-7e12c6ddfb39", networks[0].getId());
        assertEquals(NetworkType.VXLAN, networks[0].getNetworkType());
    }

    @Path("/deleteShouldSucceed")
    @POST
    public void deleteShouldSucceed() {
        LOG.debug("Calling OpenstackNeutronNetworkResource.deleteShouldSucceed()");

        String uri = String.format(URI_FORMAT, OpenstackConstants.DELETE);
        template.requestBodyAndHeader(uri, null, OpenstackConstants.ID, NETWORK_ID);
    }
}
