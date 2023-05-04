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
package org.apache.camel.quarkus.component.snm.graal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.TimeoutException;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Producer;
import org.apache.camel.component.snmp.SnmpActionType;
import org.apache.camel.component.snmp.SnmpComponent;
import org.apache.camel.component.snmp.SnmpEndpoint;
import org.apache.camel.component.snmp.SnmpMessage;
import org.apache.camel.component.snmp.SnmpProducer;
import org.apache.camel.component.snmp.SnmpTrapProducer;
import org.apache.camel.support.DefaultProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.SecurityModels;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.USM;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultTcpTransportMapping;
import org.snmp4j.transport.DefaultUdpTransportMapping;

@Recorder
public class SnmpRecorder {

    /**
     * Camel 3.18.6 uses org.apache.servicemix.bundles.snmp4j which differs in method signature of
     * SecurityModels.getInstance().addSecurityModel(this.usm)
     * compared to org.snmp4j.snmp4j.
     * For that reason CQ heeds to introduce its own SnmpProducer (with the same code as in Ca,mel 3.18.6)
     * This recorder could be removed as soon as the Camel is upgraded to 4.x (which brings org.snmp4j.snmp4j)
     */
    public RuntimeValue<SnmpComponent> configureSnmpComponent() {
        return new RuntimeValue<>(new QuarkusSnmpComponent());
    }

    @org.apache.camel.spi.annotations.Component("snmp")
    static class QuarkusSnmpComponent extends SnmpComponent {

        @Override
        protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
            SnmpEndpoint endpoint = new QuarkusSnmpEndpoint(uri, this);
            setProperties(endpoint, parameters);
            return endpoint;
        }
    }

    static class QuarkusSnmpEndpoint extends SnmpEndpoint {

        public QuarkusSnmpEndpoint(String uri, SnmpComponent component) {
            super(uri, component);
        }

        @Override
        public Producer createProducer() throws Exception {
            //code from Camel 3.18.6
            if (getType() == SnmpActionType.TRAP) {
                return new QuarkusSnmpTrapProducer(this);
            } else {
                // add the support: snmp walk (use snmp4j GET_NEXT)
                return new QuarkusSnmpProducer(this, getType());
            }
        }
    }

    //code  from Camel 3.18.6
    static class QuarkusSnmpProducer extends DefaultProducer {

        private static final Logger LOG = LoggerFactory.getLogger(SnmpProducer.class);

        private SnmpEndpoint endpoint;

        private Address targetAddress;
        private USM usm;
        private CommunityTarget target;
        private SnmpActionType actionType;
        private PDU pdu;

        public QuarkusSnmpProducer(SnmpEndpoint endpoint, SnmpActionType actionType) {
            super(endpoint);
            this.endpoint = endpoint;
            this.actionType = actionType;
        }

        @Override
        protected void doStart() throws Exception {
            super.doStart();

            this.targetAddress = GenericAddress.parse(this.endpoint.getAddress());
            LOG.debug("targetAddress: {}", targetAddress);

            this.usm = new USM(SecurityProtocols.getInstance(), new OctetString(MPv3.createLocalEngineID()), 0);
            try {
                SecurityModels.getInstance().addSecurityModel(this.usm);
            } catch (Exception e) {
                e.printStackTrace();
            }

            // setting up target
            this.target = new CommunityTarget();
            this.target.setCommunity(new OctetString(endpoint.getSnmpCommunity()));
            this.target.setAddress(this.targetAddress);
            this.target.setRetries(this.endpoint.getRetries());
            this.target.setTimeout(this.endpoint.getTimeout());
            this.target.setVersion(this.endpoint.getSnmpVersion());

            this.pdu = new PDU();
            // in here,only POLL do set the oids
            if (this.actionType == SnmpActionType.POLL) {
                for (OID oid : this.endpoint.getOids()) {
                    this.pdu.add(new VariableBinding(oid));
                }
            }
            this.pdu.setErrorIndex(0);
            this.pdu.setErrorStatus(0);
            this.pdu.setMaxRepetitions(0);
            // support POLL and GET_NEXT
            if (this.actionType == SnmpActionType.GET_NEXT) {
                this.pdu.setType(PDU.GETNEXT);
            } else {
                this.pdu.setType(PDU.GET);
            }
        }

        @Override
        protected void doStop() throws Exception {
            super.doStop();

            try {
                SecurityModels.getInstance().removeSecurityModel(new Integer32(this.usm.getID()));
            } finally {
                this.targetAddress = null;
                this.usm = null;
                this.target = null;
                this.pdu = null;
            }
        }

        @Override
        public void process(final Exchange exchange) throws Exception {
            // load connection data only if the endpoint is enabled
            Snmp snmp = null;
            TransportMapping<? extends Address> transport = null;

            try {
                LOG.debug("Starting SNMP producer on {}", this.endpoint.getAddress());

                // either tcp or udp
                if ("tcp".equals(this.endpoint.getProtocol())) {
                    transport = new DefaultTcpTransportMapping();
                } else if ("udp".equals(this.endpoint.getProtocol())) {
                    transport = new DefaultUdpTransportMapping();
                } else {
                    throw new IllegalArgumentException("Unknown protocol: " + this.endpoint.getProtocol());
                }

                snmp = new Snmp(transport);

                LOG.debug("Snmp: i am sending");

                snmp.listen();

                if (this.actionType == SnmpActionType.GET_NEXT) {
                    // snmp walk
                    List<SnmpMessage> smLst = new ArrayList<>();
                    for (OID oid : this.endpoint.getOids()) {
                        this.pdu.clear();
                        this.pdu.add(new VariableBinding(oid));

                        boolean matched = true;
                        while (matched) {
                            ResponseEvent responseEvent = snmp.send(this.pdu, this.target);
                            if (responseEvent == null || responseEvent.getResponse() == null) {
                                break;
                            }
                            PDU response = responseEvent.getResponse();
                            String nextOid = null;
                            Vector<? extends VariableBinding> variableBindings = response.getVariableBindings();
                            for (int i = 0; i < variableBindings.size(); i++) {
                                VariableBinding variableBinding = variableBindings.elementAt(i);
                                nextOid = variableBinding.getOid().toDottedString();
                                if (!nextOid.startsWith(oid.toDottedString())) {
                                    matched = false;
                                    break;
                                }
                            }
                            if (!matched) {
                                break;
                            }
                            this.pdu.clear();
                            pdu.add(new VariableBinding(new OID(nextOid)));
                            smLst.add(new SnmpMessage(getEndpoint().getCamelContext(), response));
                        }
                    }
                    exchange.getIn().setBody(smLst);
                } else {
                    // snmp get
                    ResponseEvent responseEvent = snmp.send(this.pdu, this.target);

                    LOG.debug("Snmp: sended");

                    if (responseEvent.getResponse() != null) {
                        exchange.getIn().setBody(new SnmpMessage(getEndpoint().getCamelContext(), responseEvent.getResponse()));
                    } else {
                        throw new TimeoutException("SNMP Producer Timeout");
                    }
                }
            } finally {
                try {
                    transport.close();
                } catch (Exception e) {
                }
                try {
                    snmp.close();
                } catch (Exception e) {
                }
            }
        } //end process
    }

    //code  from Camel 3.18.6
    static class QuarkusSnmpTrapProducer extends DefaultProducer {

        private static final Logger LOG = LoggerFactory.getLogger(SnmpTrapProducer.class);

        private SnmpEndpoint endpoint;

        private Address targetAddress;
        private USM usm;
        private CommunityTarget target;

        public QuarkusSnmpTrapProducer(SnmpEndpoint endpoint) {
            super(endpoint);
            this.endpoint = endpoint;
        }

        @Override
        protected void doStart() throws Exception {
            super.doStart();

            this.targetAddress = GenericAddress.parse(this.endpoint.getAddress());
            LOG.debug("targetAddress: {}", targetAddress);

            this.usm = new USM(SecurityProtocols.getInstance(), new OctetString(MPv3.createLocalEngineID()), 0);
            SecurityModels.getInstance().addSecurityModel(this.usm);

            // setting up target
            this.target = new CommunityTarget();
            this.target.setCommunity(new OctetString(endpoint.getSnmpCommunity()));
            this.target.setAddress(this.targetAddress);
            this.target.setRetries(this.endpoint.getRetries());
            this.target.setTimeout(this.endpoint.getTimeout());
            this.target.setVersion(this.endpoint.getSnmpVersion());
        }

        @Override
        protected void doStop() throws Exception {
            super.doStop();

            try {
                SecurityModels.getInstance().removeSecurityModel(new Integer32(this.usm.getID()));
            } finally {
                this.targetAddress = null;
                this.usm = null;
                this.target = null;
            }
        }

        @Override
        public void process(final Exchange exchange) throws Exception {
            // load connection data only if the endpoint is enabled
            Snmp snmp = null;
            TransportMapping<? extends Address> transport = null;

            try {
                LOG.debug("Starting SNMP Trap producer on {}", this.endpoint.getAddress());

                // either tcp or udp
                if ("tcp".equals(this.endpoint.getProtocol())) {
                    transport = new DefaultTcpTransportMapping();
                } else if ("udp".equals(this.endpoint.getProtocol())) {
                    transport = new DefaultUdpTransportMapping();
                } else {
                    throw new IllegalArgumentException("Unknown protocol: " + this.endpoint.getProtocol());
                }

                snmp = new Snmp(transport);

                LOG.debug("SnmpTrap: getting pdu from body");
                PDU trap = exchange.getIn().getBody(PDU.class);

                trap.setErrorIndex(0);
                trap.setErrorStatus(0);
                if (this.endpoint.getSnmpVersion() == SnmpConstants.version1) {
                    trap.setType(PDU.V1TRAP);
                } else {
                    trap.setType(PDU.TRAP);
                    trap.setMaxRepetitions(0);
                }

                LOG.debug("SnmpTrap: sending");
                snmp.send(trap, this.target);
                LOG.debug("SnmpTrap: sent");
            } finally {
                try {
                    transport.close();
                } catch (Exception e) {
                }
                try {
                    snmp.close();
                } catch (Exception e) {
                }
            }
        } //end process
    }

}
