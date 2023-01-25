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
package org.apache.camel.quarkus.component.soap.it.multipart;

import java.net.URL;

import javax.xml.namespace.QName;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.WebEndpoint;
import jakarta.xml.ws.WebServiceClient;
import jakarta.xml.ws.WebServiceFeature;

@WebServiceClient(name = "MultiPartCustomerServiceService", targetNamespace = "http://multipart.it.soap.component.quarkus.camel.apache.org/")
public class MultiPartCustomerServiceService extends Service {

    public final static QName SERVICE = new QName("http://multipart.it.soap.component.quarkus.camel.apache.org/",
            "MultiPartCustomerServiceService");
    public final static QName MultiPartCustomerServicePort = new QName(
            "http://multipart.it.soap.component.quarkus.camel.apache.org/", "MultiPartCustomerServicePort");

    public MultiPartCustomerServiceService(URL wsdlLocation) {
        super(wsdlLocation, SERVICE);
    }

    public MultiPartCustomerServiceService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public MultiPartCustomerServiceService() {
        super(Thread.currentThread().getContextClassLoader().getResource("/wsdl/MultiPartCustomerService.wsdl"), SERVICE);
    }

    @WebEndpoint(name = "MultiPartCustomerServicePort")
    public MultiPartCustomerService getMultiPartCustomerServicePort() {
        return super.getPort(MultiPartCustomerServicePort, MultiPartCustomerService.class);
    }

    @WebEndpoint(name = "MultiPartCustomerServicePort")
    public MultiPartCustomerService getMultiPartCustomerServicePort(WebServiceFeature... features) {
        return super.getPort(MultiPartCustomerServicePort, MultiPartCustomerService.class, features);
    }

}
