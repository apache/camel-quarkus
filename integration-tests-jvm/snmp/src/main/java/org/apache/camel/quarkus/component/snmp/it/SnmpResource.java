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
package org.apache.camel.quarkus.component.snmp.it;

import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.snmp.SnmpMessage;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.snmp4j.PDU;
import org.snmp4j.PDUv1;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TimeTicks;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;

@Path("/snmp")
@ApplicationScoped
public class SnmpResource {

    @ConfigProperty(name = SnmpRoute.TRAP_V0_PORT)
    int trap0Port;

    @ConfigProperty(name = SnmpRoute.TRAP_V1_PORT)
    int trap1Port;

    @ConfigProperty(name = "snmpListenAddress")
    String snmpListenAddress;

    @Inject
    @Named("snmpTrapResults")
    Map<String, Deque<SnmpMessage>> snmpResults;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/producePDU/{version}")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response producePDU(@PathParam("version") int version, String payload) {
        String url = String.format("snmp://%s?retries=1&snmpVersion=%d", snmpListenAddress, version);
        SnmpMessage pdu = producerTemplate.requestBody(url, version, SnmpMessage.class);

        String response = pdu.getSnmpMessage().getVariableBindings().stream()
                .map(vb -> vb.getVariable().toString())
                .collect(Collectors.joining());

        return Response.ok(response).build();
    }

    @Path("/getNext/{version}")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response getNext(String payload, @PathParam("version") int version) {
        String url = String.format("snmp://%s?type=GET_NEXT&retries=1&protocol=udp&oids=%s&snmpVersion=%d", snmpListenAddress,
                payload, version);
        List<SnmpMessage> pdu = producerTemplate.requestBody(url, "", List.class);

        String response = pdu.stream()
                .flatMap(m -> m.getSnmpMessage().getVariableBindings().stream())
                //snmp may add null oid to the result - because responder supports v3
                // (see https://camel.apache.org/components/3.20.x/snmp-component.html#_the_result_of_a_poll)
                .filter(vb -> !"Null".equals(vb.getVariable().toString()))
                .map(vb -> vb.getVariable().toString())
                .collect(Collectors.joining(","));

        return Response.ok(response).build();
    }

    @Path("/produceTrap/{version}")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response produceTrap(String payload, @PathParam("version") int version) {
        int port = new int[] { trap0Port, trap1Port, -1, -1 }[version];
        String url = "snmp:127.0.0.1:" + port + "?protocol=udp&type=TRAP&snmpVersion=" + version;
        PDU trap = createTrap(payload, version);

        producerTemplate.sendBody(url, trap);

        return Response.ok().build();
    }

    @Path("/poll/{version}")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response poll(@PathParam("version") int version,
            @QueryParam("user") String user,
            @QueryParam("securityLevel") String securityLevel,
            String oid) {
        String url = String.format("snmp:%s?protocol=udp&snmpVersion=%d&type=POLL&oids=%s", snmpListenAddress, version, oid);
        if (user != null) {
            url = url + "&securityName=" + user;
        }
        if (securityLevel != null) {
            url = url + "&securityLevel=" + securityLevel;
        }

        //Even if routeBuilder is preferred instead of consumerTemplate, consumerTemplete can be used in a case, when the component uses polling consumers by default.
        // In this case:
        // - only polling consumer is used by the SNMP component
        // - usage of the consumerTemplate reduces a lot of requests between SNMP providers significantly, thus making the tests more stable.
        Exchange e = consumerTemplate.receive(url);

        String result = e.getIn().getBody(SnmpMessage.class).getSnmpMessage().getVariableBindings().stream()
                //snmp may add null oid to the result - because responder supports v3
                // (see https://camel.apache.org/components/3.20.x/snmp-component.html#_the_result_of_a_poll)
                .filter(vb -> !"Null".equals(vb.getVariable().toString()))
                .map(v -> v.getVariable().toString()).collect(Collectors.joining(","));
        return Response.ok(result).build();
    }

    @Path("/results/{from}")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response results(@PathParam("from") String from, String oid) throws Exception {
        String result = snmpResults.get(from).stream().map(m -> {
            Variable v = m.getSnmpMessage().getVariable(new OID(oid));
            return v != null ? v.toString() : "";
        })
                .collect(Collectors.joining(","));

        return Response.ok(result).build();
    }

    public PDU createTrap(String payload, int version) {
        OID oid = new OID("1.2.3.4.5");
        Variable var = new OctetString(payload);
        switch (version) {
        case 0:
            PDUv1 trap0 = new PDUv1();
            trap0.setGenericTrap(PDUv1.ENTERPRISE_SPECIFIC);
            trap0.setSpecificTrap(1);

            trap0.add(new VariableBinding(SnmpConstants.snmpTrapOID, new OctetString(payload)));
            trap0.add(new VariableBinding(SnmpConstants.sysUpTime, new TimeTicks(5000))); // put your uptime here
            trap0.add(new VariableBinding(SnmpConstants.sysDescr, new OctetString("System Description")));
            trap0.setEnterprise(oid);

            trap0.add(new VariableBinding(oid, var));
            return trap0;
        case 1:
            PDU trap1 = new PDU();

            trap1.add(new VariableBinding(SnmpConstants.snmpTrapOID, new OctetString(payload)));
            trap1.add(new VariableBinding(SnmpConstants.sysUpTime, new TimeTicks(5000))); // put your uptime here
            trap1.add(new VariableBinding(SnmpConstants.sysDescr, new OctetString("System Description")));

            //Add Payload
            trap1.add(new VariableBinding(oid, var));
            return trap1;
        default:
            return null;
        }
    }
}
