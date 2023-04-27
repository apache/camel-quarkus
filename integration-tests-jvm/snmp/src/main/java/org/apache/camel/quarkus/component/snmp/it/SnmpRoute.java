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
                .process(e -> snmpResults.get("trap0").add(e.getIn().getBody(SnmpMessage.class)));

        //TRAP consumer snmpVersion=1
        from("snmp:0.0.0.0:" + trap1Port + "?protocol=udp&type=TRAP&snmpVersion=1")
                .process(e -> snmpResults.get("trap1").add(e.getIn().getBody(SnmpMessage.class)));

        //POLL consumer snmpVersion=0
        from("snmp://" + snmpListenAddress + "?protocol=udp&snmpVersion=0&securityName=aaa&type=POLL&oids=1.3.6.1.2.1.1.5.0")
                .process(e -> snmpResults.get("poll0").add(e.getIn().getBody(SnmpMessage.class)));
        //POLL consumer snmpVersion=1
        from("snmp://" + snmpListenAddress + "?protocol=udp&snmpVersion=1&securityName=aaa&type=POLL&oids=1.3.6.1.2.1.1.5.0")
                .process(e -> snmpResults.get("poll1").add(e.getIn().getBody(SnmpMessage.class)));
    }

    static class Producers {
        @jakarta.enterprise.inject.Produces
        @Singleton
        @Named("snmpTrapResults")
        Map<String, Deque<SnmpMessage>> snmpResults() {
            Map<String, Deque<SnmpMessage>> map = new ConcurrentHashMap<>();
            map.put("trap0", new ConcurrentLinkedDeque());
            map.put("trap1", new ConcurrentLinkedDeque());
            map.put("poll0", new ConcurrentLinkedDeque());
            map.put("poll1", new ConcurrentLinkedDeque());
            return map;
        }
    }
}
