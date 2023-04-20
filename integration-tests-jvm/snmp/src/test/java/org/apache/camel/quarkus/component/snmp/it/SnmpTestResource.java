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
import java.util.Optional;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.util.CollectionHelper;
import org.junit.jupiter.api.Assertions;
import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.MessageException;
import org.snmp4j.PDU;
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

        return CollectionHelper.mapOf(LISTEN_ADDRESS, udpTransportMapping.getListenAddress().toString().replaceFirst("/", ":"));
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
            Vector<? extends VariableBinding> vbs = Optional.ofNullable(pdu.getVariableBindings()).orElse(new Vector<>(0));
            String key = vbs.stream().sequential().map(vb -> vb.getOid().toString()).collect(Collectors.joining(","));
            int numberOfSent = counts.getOrDefault(key, 0);

            //if 3 responses were already sent for the OID, do not respond anymore
            if (numberOfSent > 3) {
                return;
            }
            //first 2 responses are quick, the third response takes 3000ms (so there is a timeout with default 1500ms) ->
            //   getNext producer will receive only 2 messages
            //   poll consumer should receive all of them
            if (numberOfSent % 3 == 2) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    //nothing
                }
            }

            PDU response = makeResponse(++numberOfSent, SnmpConstants.version1);
            if (response != null) {
                try {
                    response.setRequestID(pdu.getRequestID());
                    commandResponder.getMessageDispatcher().returnResponsePdu(
                            event.getMessageProcessingModel(), event.getSecurityModel(),
                            event.getSecurityName(), event.getSecurityLevel(),
                            response, event.getMaxSizeResponsePDU(),
                            event.getStateReference(), new StatusInformation());
                } catch (MessageException e) {
                    Assertions.assertNull(e);
                }
                counts.put(key, numberOfSent);
            }
        }

        private PDU makeResponse(int counter, int version) {
            PDU responsePDU = new PDU();
            responsePDU.setType(PDU.RESPONSE);
            responsePDU.setErrorStatus(PDU.noError);
            responsePDU.setErrorIndex(0);
            responsePDU.add(new VariableBinding(new OID(SnmpConstants.sysDescr),
                    new OctetString("Response from the test #" + counter)));
            return responsePDU;
        }
    }
}
