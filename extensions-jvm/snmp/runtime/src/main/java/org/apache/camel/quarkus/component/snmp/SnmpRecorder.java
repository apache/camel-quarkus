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
package org.apache.camel.quarkus.component.snmp;

import java.util.Map;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.Endpoint;
import org.apache.camel.Producer;
import org.apache.camel.component.snmp.SnmpActionType;
import org.apache.camel.component.snmp.SnmpComponent;
import org.apache.camel.component.snmp.SnmpEndpoint;

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
}
