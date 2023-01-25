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
import org.apache.camel.component.openstack.swift.SwiftConstants;
import org.jboss.logging.Logger;
import org.openstack4j.model.storage.object.SwiftObject;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Path("/openstack/swift/objects/")
@ApplicationScoped
public class OpenstackSwiftObjectResource {

    private static final Logger LOG = Logger.getLogger(OpenstackSwiftObjectResource.class);

    private static final String URI_FORMAT = "openstack-swift://{{camel.openstack.test.host-url}}?username=user&password=secret&project=project&operation=%s&subsystem="
            + SwiftConstants.SWIFT_SUBSYSTEM_OBJECTS;

    private static final String OBJECT_CONTAINER_NAME = "test-container";
    private static final String OBJECT_NAME = "test-file";

    @Inject
    ProducerTemplate template;

    @Path("/getShouldSucceed")
    @POST
    public void getShouldSucceed() {
        LOG.debug("Calling OpenstackSwiftObjectResource.getShouldSucceed()");

        Map<String, Object> headers = new HashMap<>();
        headers.put(SwiftConstants.CONTAINER_NAME, OBJECT_CONTAINER_NAME);
        headers.put(SwiftConstants.OBJECT_NAME, OBJECT_NAME);

        String uri = String.format(URI_FORMAT, OpenstackConstants.GET);
        SwiftObject swiftObject = template.requestBodyAndHeaders(uri, null, headers, SwiftObject.class);

        assertEquals(OBJECT_CONTAINER_NAME, swiftObject.getContainerName());
        assertEquals(OBJECT_NAME, swiftObject.getName());
        assertEquals(15, swiftObject.getSizeInBytes());
        assertEquals("application/json", swiftObject.getMimeType());
        assertEquals("12345678901234567890", swiftObject.getETag());
    }

}
