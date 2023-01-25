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
import org.openstack4j.model.network.Ipv6AddressMode;
import org.openstack4j.model.network.Ipv6RaMode;
import org.openstack4j.model.network.Subnet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Path("/openstack/neutron/subnets/")
@ApplicationScoped
public class OpenstackNeutronSubnetResource {

    private static final Logger LOG = Logger.getLogger(OpenstackNeutronSubnetResource.class);

    private static final String URI_FORMAT = "openstack-neutron://{{camel.openstack.test.host-url}}?username=user&password=secret&project=project&operation=%s&subsystem="
            + NeutronConstants.NEUTRON_SUBNETS_SYSTEM;

    private static final String SUBNET_NAME = "sub1";
    private static final String SUBNET_ID = "3b80198d-4f7b-4f77-9ef5-774d54e17126";

    @Inject
    ProducerTemplate template;

    @Path("/getShouldSucceed")
    @POST
    public void getShouldSucceed() {
        LOG.debug("Calling OpenstackNeutronSubnetResource.getShouldSucceed()");

        String uri = String.format(URI_FORMAT, OpenstackConstants.GET);
        Subnet out = template.requestBodyAndHeader(uri, null, OpenstackConstants.ID, SUBNET_ID, Subnet.class);

        assertNotNull(out);
        assertEquals(SUBNET_NAME, out.getName());
        assertEquals(Ipv6AddressMode.DHCPV6_STATEFUL, out.getIpv6AddressMode());
        assertEquals(Ipv6RaMode.DHCPV6_STATEFUL, out.getIpv6RaMode());
        assertNotNull(out.getDnsNames());
        assertTrue(out.getDnsNames().isEmpty());
    }
}
