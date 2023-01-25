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

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;
import jakarta.xml.bind.annotation.XmlSeeAlso;

@WebService(targetNamespace = "http://service.it.soap.component.quarkus.camel.apache.org/", name = "CustomerService")
@XmlSeeAlso({ ObjectFactory.class })
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
public interface CustomerService {

    @WebMethod(action = "http://service.it.soap.component.quarkus.camel.apache.org/getAllAmericanCustomers")
    @WebResult(name = "getAllAmericanCustomersResponse", targetNamespace = "http://service.it.soap.component.quarkus.camel.apache.org/", partName = "parameters")
    public GetAllAmericanCustomersResponse getAllAmericanCustomers();

    @WebMethod(action = "http://service.it.soap.component.quarkus.camel.apache.org/saveCustomer")
    public void saveCustomer(

            @WebParam(partName = "parameters", name = "saveCustomer", targetNamespace = "http://service.it.soap.component.quarkus.camel.apache.org/") SaveCustomer parameters);

    @WebMethod(action = "http://service.it.soap.component.quarkus.camel.apache.org/getCustomersByName")
    @WebResult(name = "getCustomersByNameResponse", targetNamespace = "http://service.it.soap.component.quarkus.camel.apache.org/", partName = "parameters")
    public GetCustomersByNameResponse getCustomersByName(

            @WebParam(partName = "parameters", name = "getCustomersByName", targetNamespace = "http://service.it.soap.component.quarkus.camel.apache.org/") GetCustomersByName parameters)
            throws NoSuchCustomerException;

    @WebMethod(action = "http://service.it.soap.component.quarkus.camel.apache.org/getAllCustomers")
    @WebResult(name = "getAllCustomersResponse", targetNamespace = "http://service.it.soap.component.quarkus.camel.apache.org/", partName = "parameters")
    public GetAllCustomersResponse getAllCustomers();
}
