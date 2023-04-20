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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Produces;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.snmp.SnmpMessage;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class SnmpRoute extends RouteBuilder {

    @ConfigProperty(name = "snmpListenAddress")
    String snmpListenAddress;

    @Inject
    @Named("snmpTrapResults")
    Map<String, Deque<SnmpMessage>> snmpResults;

    @Override
    public void configure() {
        //TRAP consumer
        from("snmp:0.0.0.0:1662?protocol=udp&type=TRAP&snmpVersion=0")
                .process(e -> snmpResults.get("trap").add(e.getIn().getBody(SnmpMessage.class)));

        //POLL consumer
        from("snmp://" + snmpListenAddress + "?protocol=udp&type=POLL&snmpVersion=0&oids=1.3.6.1.2.1.1.5.0")
                .process(e -> snmpResults.get("poll").add(e.getIn().getBody(SnmpMessage.class)));
    }

    static class Producers {
        @Produces
        @Singleton
        @Named("snmpTrapResults")
        Map<String, Deque<SnmpMessage>> snmpResults() {
            Map<String, Deque<SnmpMessage>> map = new ConcurrentHashMap<>();
            map.put("trap", new ConcurrentLinkedDeque());
            map.put("poll", new ConcurrentLinkedDeque());
            return map;
        }
    }
}
