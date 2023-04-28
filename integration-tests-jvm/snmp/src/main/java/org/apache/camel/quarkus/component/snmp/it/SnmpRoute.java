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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.snmp.SnmpMessage;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.snmp4j.mp.SnmpConstants;

@ApplicationScoped
public class SnmpRoute extends RouteBuilder {

    public static final String TRAP_V0_PORT = "SnmpRoute_trap_v0";
    public static final String TRAP_V1_PORT = "SnmpRoute_trap_v1";

    @ConfigProperty(name = TRAP_V0_PORT)
    int trap0Port;

    @ConfigProperty(name = "SnmpRoute_trap_v1")
    int trap1Port;

    @ConfigProperty(name = "snmpListenAddress")
    String snmpListenAddress;

    @Inject
    @Named("snmpTrapResults")
    Map<String, Deque<SnmpMessage>> snmpResults;

    @Override
    public void configure() {
        //TRAP consumer snmpVersion=0
        from("snmp:0.0.0.0:" + trap0Port + "?protocol=udp&type=TRAP&snmpVersion=0")
                .process(e -> snmpResults.get("v0_trap").add(e.getIn().getBody(SnmpMessage.class)));

        //TRAP consumer snmpVersion=1
        from("snmp:0.0.0.0:" + trap1Port + "?protocol=udp&type=TRAP&snmpVersion=1")
                .process(e -> snmpResults.get("v1_trap").add(e.getIn().getBody(SnmpMessage.class)));

        //POLL consumer 2 oidps, snmpVersion=0
        from("snmp://" + snmpListenAddress + "?protocol=udp&snmpVersion=0&type=POLL&oids=" +
                SnmpConstants.sysName + "," + SnmpConstants.sysContact)
                        .process(e -> snmpResults.get("v0_poll2oids").add(e.getIn().getBody(SnmpMessage.class)));
        //POLL consumer 2 oidps, snmpVersion=1
        from("snmp://" + snmpListenAddress + "?protocol=udp&snmpVersion=1&type=POLL&oids=" +
                SnmpConstants.sysName + "," + SnmpConstants.sysContact)
                        .process(e -> snmpResults.get("v1_poll2oids").add(e.getIn().getBody(SnmpMessage.class)));

        //POLL consumer starting with dot snmpVersion=0
        from("snmp://" + snmpListenAddress
                + "?protocol=udp&snmpVersion=0&type=POLL&oids=.1.3.6.1.4.1.6527.3.1.2.21.2.1.50")
                        .process(e -> snmpResults.get("v0_pollStartingDot").add(e.getIn().getBody(SnmpMessage.class)));
        //POLL consumer startingWith dot snmpVersion=1
        from("snmp://" + snmpListenAddress
                + "?protocol=udp&snmpVersion=1&type=POLL&oids=.1.3.6.1.4.1.6527.3.1.2.21.2.1.50")
                        .process(e -> snmpResults.get("v1_pollStartingDot").add(e.getIn().getBody(SnmpMessage.class)));

        //POLL consumer snmpVersion=0
        from("snmp://" + snmpListenAddress + "?protocol=udp&snmpVersion=0&type=POLL&oids="
                + SnmpConstants.sysName)
                        .process(e -> snmpResults.get("v0_poll").add(e.getIn().getBody(SnmpMessage.class)));
        //POLL consumer snmpVersion=1
        from("snmp://" + snmpListenAddress + "?protocol=udp&snmpVersion=1&type=POLL&oids="
                + SnmpConstants.sysName)
                        .process(e -> snmpResults.get("v1_poll").add(e.getIn().getBody(SnmpMessage.class)));
    }

    static class Producers {
        @jakarta.enterprise.inject.Produces
        @Singleton
        @Named("snmpTrapResults")
        Map<String, Deque<SnmpMessage>> snmpResults() {
            Map<String, Deque<SnmpMessage>> map = new ConcurrentHashMap<>();
            map.put("v0_trap", new ConcurrentLinkedDeque());
            map.put("v1_trap", new ConcurrentLinkedDeque());
            map.put("v0_poll", new ConcurrentLinkedDeque());
            map.put("v1_poll", new ConcurrentLinkedDeque());
            map.put("v3_poll", new ConcurrentLinkedDeque());
            map.put("v0_pollStartingDot", new ConcurrentLinkedDeque());
            map.put("v1_pollStartingDot", new ConcurrentLinkedDeque());
            map.put("v0_poll2oids", new ConcurrentLinkedDeque());
            map.put("v1_poll2oids", new ConcurrentLinkedDeque());
            return map;
        }
    }
}
