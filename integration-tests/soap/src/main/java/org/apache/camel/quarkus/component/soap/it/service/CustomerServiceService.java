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
package org.apache.camel.quarkus.component.soap.it.service;

import java.net.URL;

import javax.xml.namespace.QName;

import jakarta.xml.ws.Service;
import jakarta.xml.ws.WebEndpoint;
import jakarta.xml.ws.WebServiceClient;
import jakarta.xml.ws.WebServiceFeature;

@WebServiceClient(name = "CustomerServiceService", targetNamespace = "http://service.it.soap.component.quarkus.camel.apache.org/")
public class CustomerServiceService extends Service {

    public final static QName SERVICE = new QName("http://service.it.soap.component.quarkus.camel.apache.org/",
            "CustomerServiceService");
    public final static QName CustomerServicePort = new QName("http://service.it.soap.component.quarkus.camel.apache.org/",
            "CustomerServicePort");

    public CustomerServiceService(URL wsdlLocation) {
        super(wsdlLocation, SERVICE);
    }

    public CustomerServiceService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public CustomerServiceService() {
        super(Thread.currentThread().getContextClassLoader().getResource("/wsdl/CustomerService.wsdl"), SERVICE);
    }

    @WebEndpoint(name = "CustomerServicePort")
    public CustomerService getCustomerServicePort() {
        return super.getPort(CustomerServicePort, CustomerService.class);
    }

    @WebEndpoint(name = "CustomerServicePort")
    public CustomerService getCustomerServicePort(WebServiceFeature... features) {
        return super.getPort(CustomerServicePort, CustomerService.class, features);
    }

}
