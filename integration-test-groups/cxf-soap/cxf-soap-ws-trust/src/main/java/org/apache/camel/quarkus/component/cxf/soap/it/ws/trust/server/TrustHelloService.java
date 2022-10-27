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
package org.apache.camel.quarkus.component.cxf.soap.it.ws.trust.server;

import javax.jws.WebMethod;
import javax.jws.WebService;

import org.apache.cxf.annotations.Policies;
import org.apache.cxf.annotations.Policy;

@WebService(targetNamespace = "https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test/ws-trust")
@Policy(placement = Policy.Placement.BINDING, uri = "classpath:/AsymmetricSAML2Policy.xml")
public interface TrustHelloService {
    @WebMethod
    @Policies({
            @Policy(placement = Policy.Placement.BINDING_OPERATION_INPUT, uri = "classpath:/Input_Policy.xml"),
            @Policy(placement = Policy.Placement.BINDING_OPERATION_OUTPUT, uri = "classpath:/Output_Policy.xml")
    })
    String sayHello();
}
