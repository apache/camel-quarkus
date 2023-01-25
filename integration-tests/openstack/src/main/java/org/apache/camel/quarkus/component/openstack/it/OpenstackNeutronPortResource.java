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
import org.openstack4j.model.network.AllowedAddressPair;
import org.openstack4j.model.network.Port;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Path("/openstack/neutron/ports/")
@ApplicationScoped
public class OpenstackNeutronPortResource {

    private static final Logger LOG = Logger.getLogger(OpenstackNeutronPortResource.class);

    private static final String URI_FORMAT = "openstack-neutron://{{camel.openstack.test.host-url}}?username=user&password=secret&project=project&operation=%s&subsystem="
            + NeutronConstants.NEUTRON_PORT_SYSTEM;

    private static final String NETWORK_ID = "a87cc70a-3e15-4acf-8205-9b711a3531b7";

    @Inject
    ProducerTemplate template;

    @Path("/createShouldSucceed")
    @POST
    public void createShouldSucceed() {
        LOG.debug("Calling OpenstackNeutronPortResource.createShouldSucceed()");

        Port in = Builders.port().networkId(NETWORK_ID).build();

        String uri = String.format(URI_FORMAT, OpenstackConstants.CREATE);
        Port out = template.requestBody(uri, in, Port.class);

        assertNotNull(out);
        assertEquals(NETWORK_ID, out.getNetworkId());
        assertNotNull(out.getAllowedAddressPairs());
        assertEquals(1, out.getAllowedAddressPairs().size());
        AllowedAddressPair allowedAddressPair = out.getAllowedAddressPairs().iterator().next();
        assertNotNull(allowedAddressPair.getIpAddress());
        assertNotNull(allowedAddressPair.getMacAddress());
    }

    @Path("/getAllShouldSucceed")
    @POST
    public void getAllShouldSucceed() {
        LOG.debug("Calling OpenstackNeutronPortResource.getAllShouldSucceed()");

        String uri = String.format(URI_FORMAT, OpenstackConstants.GET_ALL);
        Port[] ports = template.requestBody(uri, null, Port[].class);

        assertNotNull(ports);
        assertEquals(2, ports.length);
        assertEquals(NETWORK_ID, ports[0].getNetworkId());
        assertEquals("94225baa-9d3f-4b93-bf12-b41e7ce49cdb", ports[0].getId());
        assertEquals(NETWORK_ID, ports[1].getNetworkId());
        assertEquals("235b09e0-63c4-47f1-b221-66ba54c21760", ports[1].getId());
    }
}
