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
package org.apache.camel.quarkus.component.sap.netweaver.it;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.sap.netweaver.NetWeaverConstants;

@Path("/sap-netweaver")
public class SapNetweaverResource {

    private static final String SAP_COMMAND = "FlightCollection(carrid='AA',connid='0017',fldate=datetime'2017-10-05T00%3A00%3A00')";

    @Inject
    ProducerTemplate producerTemplate;

    @GET
    @Path("/json")
    @Produces(MediaType.TEXT_PLAIN)
    public String getSapNetweaverJson(@QueryParam("test-port") int port) {
        return producerTemplate.requestBodyAndHeader("sap-netweaver:http://localhost:" + port + "/sap/api/json", null,
                NetWeaverConstants.COMMAND, SAP_COMMAND, String.class);
    }

    @GET
    @Path("/xml")
    @Produces(MediaType.TEXT_PLAIN)
    public String getSapNetweaverXml(@QueryParam("test-port") int port) {
        return producerTemplate.requestBodyAndHeader("sap-netweaver:http://localhost:" + port + "/sap/api/xml?json=false", null,
                NetWeaverConstants.COMMAND, SAP_COMMAND, String.class);
    }
}
