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
package org.apache.camel.quarkus.component.cxf.soap.securitypolicy.server.cxf.way.it;

import jakarta.inject.Inject;
import jakarta.jws.WebService;

import org.apache.camel.ProducerTemplate;
import org.apache.cxf.annotations.EndpointProperties;
import org.apache.cxf.annotations.EndpointProperty;
import org.apache.cxf.annotations.Policy;

@WebService(portName = "EncryptSecurityServicePort", serviceName = "WssSecurityPolicyHelloServiceCxfWay", targetNamespace = "https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/ws-securitypolicy", endpointInterface = "org.apache.camel.quarkus.component.cxf.soap.securitypolicy.server.cxf.way.it.WssSecurityPolicyHelloServiceCxfWay")
@Policy(placement = Policy.Placement.BINDING, uri = "encrypt-sign-policy.xml")
@EndpointProperties(value = {
        @EndpointProperty(key = "ws-security.signature.properties", value = "bob.properties"),
        @EndpointProperty(key = "ws-security.encryption.properties", value = "bob.properties"),
        @EndpointProperty(key = "ws-security.signature.username", value = "bob"),
        @EndpointProperty(key = "ws-security.encryption.username", value = "alice"),
        @EndpointProperty(key = "ws-security.callback-handler", value = "org.apache.camel.quarkus.component.cxf.soap.securitypolicy.server.it.PasswordCallbackHandler")
})
public class WssSecurityPolicyHelloServiceCxfWayImpl implements WssSecurityPolicyHelloServiceCxfWay {

    @Inject
    ProducerTemplate producerTemplate;

    public String sayHello(String name) {
        return producerTemplate.requestBody("direct:wsSecurityPolicyCxfWay", name, String.class);
    }
}
