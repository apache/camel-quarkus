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
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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

    @ConfigProperty(name = "snmpListenAddress")
    String snmpListenAddress;

    @Inject
    @Named("snmpTrapResults")
    Map<String, Deque<SnmpMessage>> snmpResults;

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/producePDU")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response producePDU() {
        String url = String.format("snmp://%s?retries=1", snmpListenAddress);
        SnmpMessage pdu = producerTemplate.requestBody(url, "", SnmpMessage.class);

        String response = pdu.getSnmpMessage().getVariableBindings().stream()
                .filter(vb -> vb.getOid().equals(SnmpConstants.sysDescr))
                .map(vb -> vb.getVariable().toString())
                .collect(Collectors.joining());

        return Response.ok(response).build();
    }

    @Path("/sendPoll")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response sendPoll() {
        SnmpMessage pdu = producerTemplate.requestBody("direct:producePoll", "", SnmpMessage.class);

        String response = pdu.getSnmpMessage().getVariableBindings().stream()
                .filter(vb -> vb.getOid().equals(SnmpConstants.sysDescr))
                .map(vb -> vb.getVariable().toString())
                .collect(Collectors.joining());

        return Response.ok(response).build();
    }

    @Path("/getNext")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response getNext(String payload) {
        String url = String.format("snmp://%s?type=GET_NEXT&retries=1&protocol=udp&oids=%s", snmpListenAddress,
                SnmpConstants.sysDescr);
        List<SnmpMessage> pdu = producerTemplate.requestBody(url, "", List.class);

        String response = pdu.stream()
                .flatMap(m -> m.getSnmpMessage().getVariableBindings().stream())
                .filter(vb -> vb.getOid().equals(SnmpConstants.sysDescr))
                .map(vb -> vb.getVariable().toString())
                .collect(Collectors.joining(","));

        return Response.ok(response).build();
    }

    @Path("/produceTrap")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response sendTrap(String payload) {
        String url = "snmp:127.0.0.1:1662?protocol=udp&type=TRAP&snmpVersion=0)";
        PDU trap = createTrap(payload);

        producerTemplate.sendBody(url, trap);

        return Response.ok().build();
    }

    @Path("/results")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response results(String from) throws Exception {
        OID oid = "trap".equals(from) ? new OID("1.2.3.4.5") : SnmpConstants.sysDescr;
        String result = snmpResults.get(from).stream().map(m -> m.getSnmpMessage().getVariable(oid).toString())
                .collect(Collectors.joining(","));

        return Response.ok(result).build();
    }

    public PDU createTrap(String payload) {
        PDUv1 trap = new PDUv1();
        trap.setGenericTrap(PDUv1.ENTERPRISE_SPECIFIC);
        trap.setSpecificTrap(1);

        OID oid = new OID("1.2.3.4.5");
        trap.add(new VariableBinding(SnmpConstants.snmpTrapOID, oid));
        trap.add(new VariableBinding(SnmpConstants.sysUpTime, new TimeTicks(5000))); // put your uptime here
        trap.add(new VariableBinding(SnmpConstants.sysDescr, new OctetString("System Description")));
        trap.setEnterprise(oid);

        //Add Payload
        Variable var = new OctetString(payload);
        trap.add(new VariableBinding(oid, var));
        return trap;
    }
}
