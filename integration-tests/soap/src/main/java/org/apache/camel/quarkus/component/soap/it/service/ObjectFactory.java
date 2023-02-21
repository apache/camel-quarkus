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

import javax.xml.namespace.QName;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlElementDecl;
import jakarta.xml.bind.annotation.XmlRegistry;

@XmlRegistry
public class ObjectFactory {

    private final static QName _GetCustomersByName_QNAME = new QName(
            "http://service.it.soap.component.quarkus.camel.apache.org/",
            "getCustomersByName");
    private final static QName _GetCustomersByNameResponse_QNAME = new QName(
            "http://service.it.soap.component.quarkus.camel.apache.org/",
            "getCustomersByNameResponse");
    private final static QName _NoSuchCustomer_QNAME = new QName("http://service.it.soap.component.quarkus.camel.apache.org/",
            "NoSuchCustomer");
    private final static QName _GetAllCustomers_QNAME = new QName("http://service.it.soap.component.quarkus.camel.apache.org/",
            "getAllCustomers");
    private final static QName _GetAllCustomersResponse_QNAME = new QName(
            "http://service.it.soap.component.quarkus.camel.apache.org/",
            "getAllCustomersResponse");
    private final static QName _GetAllAmericanCustomers_QNAME = new QName(
            "http://service.it.soap.component.quarkus.camel.apache.org/",
            "getAllAmericanCustomers");
    private final static QName _GetAllAmericanCustomersResponse_QNAME = new QName(
            "http://service.it.soap.component.quarkus.camel.apache.org/",
            "getAllAmericanCustomersResponse");
    private final static QName _SaveCustomer_QNAME = new QName("http://service.it.soap.component.quarkus.camel.apache.org/",
            "saveCustomer");

    public ObjectFactory() {
    }

    public GetCustomersByName createGetCustomersByName() {
        return new GetCustomersByName();
    }

    public GetCustomersByNameResponse createGetCustomersByNameResponse() {
        return new GetCustomersByNameResponse();
    }

    public NoSuchCustomer createNoSuchCustomer() {
        return new NoSuchCustomer();
    }

    public GetAllCustomersResponse createGetAllCustomersResponse() {
        return new GetAllCustomersResponse();
    }

    public GetAllAmericanCustomersResponse createGetAllAmericanCustomersResponse() {
        return new GetAllAmericanCustomersResponse();
    }

    public SaveCustomer createSaveCustomer() {
        return new SaveCustomer();
    }

    public Customer createCustomer() {
        return new Customer();
    }

    @XmlElementDecl(namespace = "http://service.it.soap.component.quarkus.camel.apache.org/", name = "getCustomersByName")
    public JAXBElement<GetCustomersByName> createGetCustomersByName(GetCustomersByName value) {
        return new JAXBElement<GetCustomersByName>(_GetCustomersByName_QNAME, GetCustomersByName.class, null, value);
    }

    @XmlElementDecl(namespace = "http://service.it.soap.component.quarkus.camel.apache.org/", name = "getCustomersByNameResponse")
    public JAXBElement<GetCustomersByNameResponse> createGetCustomersByNameResponse(GetCustomersByNameResponse value) {
        return new JAXBElement<GetCustomersByNameResponse>(_GetCustomersByNameResponse_QNAME, GetCustomersByNameResponse.class,
                null, value);
    }

    @XmlElementDecl(namespace = "http://service.it.soap.component.quarkus.camel.apache.org/", name = "NoSuchCustomer")
    public JAXBElement<NoSuchCustomer> createNoSuchCustomer(NoSuchCustomer value) {
        return new JAXBElement<NoSuchCustomer>(_NoSuchCustomer_QNAME, NoSuchCustomer.class, null, value);
    }

    @XmlElementDecl(namespace = "http://service.it.soap.component.quarkus.camel.apache.org/", name = "getAllCustomers")
    public JAXBElement<Object> createGetAllCustomers(Object value) {
        return new JAXBElement<Object>(_GetAllCustomers_QNAME, Object.class, null, value);
    }

    @XmlElementDecl(namespace = "http://service.it.soap.component.quarkus.camel.apache.org/", name = "getAllCustomersResponse")
    public JAXBElement<GetAllCustomersResponse> createGetAllCustomersResponse(GetAllCustomersResponse value) {
        return new JAXBElement<GetAllCustomersResponse>(_GetAllCustomersResponse_QNAME, GetAllCustomersResponse.class, null,
                value);
    }

    @XmlElementDecl(namespace = "http://service.it.soap.component.quarkus.camel.apache.org/", name = "getAllAmericanCustomers")
    public JAXBElement<Object> createGetAllAmericanCustomers(Object value) {
        return new JAXBElement<Object>(_GetAllAmericanCustomers_QNAME, Object.class, null, value);
    }

    @XmlElementDecl(namespace = "http://service.it.soap.component.quarkus.camel.apache.org/", name = "getAllAmericanCustomersResponse")
    public JAXBElement<GetAllAmericanCustomersResponse> createGetAllAmericanCustomersResponse(
            GetAllAmericanCustomersResponse value) {
        return new JAXBElement<GetAllAmericanCustomersResponse>(_GetAllAmericanCustomersResponse_QNAME,
                GetAllAmericanCustomersResponse.class, null, value);
    }

    @XmlElementDecl(namespace = "http://service.it.soap.component.quarkus.camel.apache.org/", name = "saveCustomer")
    public JAXBElement<SaveCustomer> createSaveCustomer(SaveCustomer value) {
        return new JAXBElement<SaveCustomer>(_SaveCustomer_QNAME, SaveCustomer.class, null, value);
    }

}
