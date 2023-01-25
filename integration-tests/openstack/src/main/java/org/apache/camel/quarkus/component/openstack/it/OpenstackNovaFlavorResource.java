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
import org.apache.camel.component.openstack.nova.NovaConstants;
import org.jboss.logging.Logger;
import org.openstack4j.api.Builders;
import org.openstack4j.model.compute.Flavor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Path("/openstack/nova/flavors/")
@ApplicationScoped
public class OpenstackNovaFlavorResource {

    private static final Logger LOG = Logger.getLogger(OpenstackNovaFlavorResource.class);

    private static final String URI_FORMAT = "openstack-nova://{{camel.openstack.test.host-url}}?username=user&password=secret&project=project&operation=%s&subsystem="
            + NovaConstants.NOVA_SUBSYSTEM_FLAVORS;

    private static final String FLAVOR_ID = "1";
    private static final String FLAVOR_NAME = "m1.tiny";

    @Inject
    ProducerTemplate template;

    @Path("/createShouldSucceed")
    @POST
    public void createShouldSucceed() {
        LOG.debug("Calling OpenstackNovaFlavorResource.createShouldSucceed()");

        Flavor in = Builders.flavor().name("safe_to_delete_flavor").vcpus(1).disk(2).isPublic(true).rxtxFactor(2.0f)
                .ephemeral(1)
                .ram(128).id("delete_1").swap(1).build();

        String uri = String.format(URI_FORMAT, OpenstackConstants.CREATE);
        Flavor out = template.requestBody(uri, in, Flavor.class);

        assertNotNull(out);
        assertEquals(1, out.getVcpus());
        assertEquals(2, out.getDisk());
        assertEquals(FLAVOR_NAME, out.getName());
        assertTrue(out.isPublic());
        assertEquals(2.0f, out.getRxtxFactor());
        assertEquals(1, out.getEphemeral());
        assertEquals(128, out.getRam());
        assertEquals(FLAVOR_ID, out.getId());
        assertEquals(1, out.getSwap());
    }

    @Path("/getShouldSucceed")
    @POST
    public void getShouldSucceed() {
        LOG.debug("Calling OpenstackNovaFlavorResource.getShouldSucceed()");

        String uri = String.format(URI_FORMAT, OpenstackConstants.GET);
        Flavor out = template.requestBodyAndHeader(uri, null, OpenstackConstants.ID, FLAVOR_ID, Flavor.class);

        assertNotNull(out);
        assertEquals(1, out.getDisk());
        assertEquals(FLAVOR_NAME, out.getName());
        assertEquals(512, out.getRam());
        assertTrue(out.isPublic());
        assertEquals(0, out.getEphemeral());
        assertFalse(out.isDisabled());
        assertEquals(2.0f, out.getRxtxFactor());
        assertEquals(1, out.getVcpus());
    }

    @Path("/getAllShouldSucceed")
    @POST
    public void getAllShouldSucceed() {
        LOG.debug("Calling OpenstackNovaFlavorResource.getAllShouldSucceed()");

        String uri = String.format(URI_FORMAT, OpenstackConstants.GET_ALL);
        Flavor[] flavors = template.requestBody(uri, null, Flavor[].class);

        assertNotNull(flavors);
        assertEquals(2, flavors.length);
    }

}
