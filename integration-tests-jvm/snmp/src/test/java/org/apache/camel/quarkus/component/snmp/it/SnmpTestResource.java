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

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.quarkus.test.AvailablePortFinder;
import org.apache.camel.util.CollectionHelper;
import org.junit.jupiter.api.Assertions;
import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.MessageException;
import org.snmp4j.PDU;
import org.snmp4j.PDUv1;
import org.snmp4j.ScopedPDU;
import org.snmp4j.Snmp;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.mp.StatusInformation;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

public class SnmpTestResource implements QuarkusTestResourceLifecycleManager {

    public static final String LISTEN_ADDRESS = "snmpListenAddress";
    public static final String LOCAL_ADDRESS = "127.0.0.1/0";

    Snmp snmpResponder;

    @Override
    public Map<String, String> start() {
        DefaultUdpTransportMapping udpTransportMapping;
        try {
            udpTransportMapping = new DefaultUdpTransportMapping(new UdpAddress(LOCAL_ADDRESS));
            snmpResponder = new Snmp(udpTransportMapping);

            TestCommandResponder responder = new TestCommandResponder(snmpResponder);
            snmpResponder.addCommandResponder(responder);

            snmpResponder.listen();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Map<String, String> ports = AvailablePortFinder.reserveNetworkPorts(Objects::toString, SnmpRoute.TRAP_V0_PORT,
                SnmpRoute.TRAP_V1_PORT);
        Map<String, String> m = CollectionHelper.mergeMaps(
                ports,
                CollectionHelper.mapOf(LISTEN_ADDRESS,
                        udpTransportMapping.getListenAddress().toString().replaceFirst("/", ":")));
        return m;
    }

    @Override
    public void stop() {
        if (snmpResponder != null) {
            try {
                snmpResponder.close();
            } catch (IOException e) {
                //do nothing
            }
        }
    }

    static class TestCommandResponder implements CommandResponder {

        private final Snmp commandResponder;
        private final Map<String, Integer> counts = new ConcurrentHashMap<>();

        public TestCommandResponder(Snmp commandResponder) {
            this.commandResponder = commandResponder;
        }

        @Override
        public synchronized void processPdu(CommandResponderEvent event) {
            PDU pdu = event.getPDU();
            Vector<? extends VariableBinding> vbs;
            if (pdu.getVariableBindings() != null) {
                vbs = new Vector<>(pdu.getVariableBindings());
            } else {
                vbs = new Vector<>(0);
            }
            String key = vbs.stream().sequential().map(vb -> vb.getOid().toString()).collect(Collectors.joining(","));

            //differ snmp versions
            if (pdu instanceof PDUv1) {
                key = "v1_" + key;
            } else if (pdu instanceof ScopedPDU) {
                key = "v3_" + key;
            } else {
                key = "v2_" + key;
            }
            int numberOfSent = counts.getOrDefault(key, 0);

            try {
                PDU response = makeResponse(++numberOfSent, SnmpConstants.version1, vbs);
                if (response != null) {
                    response.setRequestID(pdu.getRequestID());
                    commandResponder.getMessageDispatcher().returnResponsePdu(
                            event.getMessageProcessingModel(), event.getSecurityModel(),
                            event.getSecurityName(), event.getSecurityLevel(),
                            response, event.getMaxSizeResponsePDU(),
                            event.getStateReference(), new StatusInformation());
                }
            } catch (MessageException e) {
                Assertions.assertNull(e);
            }
            counts.put(key, numberOfSent);
        }

        private PDU makeResponse(int counter, int version, Vector<? extends VariableBinding> vbs) {
            PDU responsePDU = new PDU();
            responsePDU.setType(PDU.RESPONSE);
            responsePDU.setErrorStatus(PDU.noError);
            responsePDU.setErrorIndex(0);
            if (vbs.isEmpty()) {
                VariableBinding vb = generateResponseBinding(counter, SnmpTest.PRODUCE_PDU_OID);
                if (vb != null) {
                    responsePDU.add(vb);
                }
            } else {
                vbs.stream().forEach(vb -> responsePDU.add(generateResponseBinding(counter, vb.getOid())));
            }
            if (responsePDU.getVariableBindings().isEmpty()) {
                return null;
            }
            return responsePDU;
        }

        private VariableBinding generateResponseBinding(int counter, OID oid) {
            //get next test
            if (SnmpTest.GET_NEXT_OID.equals(oid)) {
                //if counter < 2 return the same oid
                if (counter < 3) {
                    return new VariableBinding(SnmpTest.GET_NEXT_OID, new OctetString("" + counter));
                }
                if (counter == 3) {
                    //else return sysDescr
                    return new VariableBinding(SnmpTest.GET_NEXT_OID,
                            new OctetString("My GET_NEXT Printer - response #" + counter));
                }
                //else do not send response
                return null;
            }

            if (SnmpTest.POLL_OID.equals(oid)) {
                if (counter < 4) {
                    return new VariableBinding(SnmpTest.POLL_OID,
                            new OctetString("My POLL Printer - response #" + counter));
                }

            }

            if (SnmpTest.PRODUCE_PDU_OID.equals(oid)) {
                if (counter < 4) {
                    return new VariableBinding(SnmpTest.PRODUCE_PDU_OID,
                            new OctetString("My PRODUCE_PDU Printer - response #" + counter));
                }
            }

            if (SnmpTest.TWO_OIDS_A.equals(oid)) {
                if (counter < 4) {
                    return new VariableBinding(SnmpTest.TWO_OIDS_A,
                            new OctetString("My 2 OIDs A Printer - response #" + counter));
                }
            }

            if (SnmpTest.TWO_OIDS_B.equals(oid)) {
                if (counter < 4) {
                    return new VariableBinding(SnmpTest.TWO_OIDS_B,
                            new OctetString("My 2 OIDs B Printer - response #" + counter));
                }
            }

            if (SnmpTest.DOT_OID.equals(oid)) {
                if (counter < 4) {
                    return new VariableBinding(SnmpTest.DOT_OID,
                            new OctetString("My DOT Printer - response #" + counter));
                }
            }

            return null;
        }
    }
}
