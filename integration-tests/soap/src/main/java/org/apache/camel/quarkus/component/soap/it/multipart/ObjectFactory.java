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

import javax.xml.namespace.QName;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlElementDecl;
import jakarta.xml.bind.annotation.XmlRegistry;

@XmlRegistry
public class ObjectFactory {

    private final static QName _GetCustomersByName_QNAME = new QName(
            "http://multipart.it.soap.component.quarkus.camel.apache.org/", "getCustomersByName");
    private final static QName _GetCustomersByNameResponse_QNAME = new QName(
            "http://multipart.it.soap.component.quarkus.camel.apache.org/", "getCustomersByNameResponse");
    private final static QName _NoSuchCustomer_QNAME = new QName("http://multipart.it.soap.component.quarkus.camel.apache.org/",
            "NoSuchCustomer");
    private final static QName _GetAllCustomers_QNAME = new QName(
            "http://multipart.it.soap.component.quarkus.camel.apache.org/", "getAllCustomers");
    private final static QName _GetAllCustomersResponse_QNAME = new QName(
            "http://multipart.it.soap.component.quarkus.camel.apache.org/", "getAllCustomersResponse");
    private final static QName _SaveCustomer_QNAME = new QName("http://multipart.it.soap.component.quarkus.camel.apache.org/",
            "saveCustomer");
    private final static QName _SaveCustomerToo_QNAME = new QName(
            "http://multipart.it.soap.component.quarkus.camel.apache.org/", "saveCustomerToo");
    private final static QName _Company_QNAME = new QName("http://multipart.it.soap.component.quarkus.camel.apache.org/",
            "company");
    private final static QName _CompanyType_QNAME = new QName("http://multipart.it.soap.component.quarkus.camel.apache.org/",
            "companyType");
    private final static QName _Product_QNAME = new QName("http://multipart.it.soap.component.quarkus.camel.apache.org/",
            "product");

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

    public SaveCustomer createSaveCustomer() {
        return new SaveCustomer();
    }

    public Company createCompany() {
        return new Company();
    }

    public Product createProduct() {
        return new Product();
    }

    public Customer createCustomer() {
        return new Customer();
    }

    @XmlElementDecl(namespace = "http://multipart.it.soap.component.quarkus.camel.apache.org/", name = "getCustomersByName")
    public JAXBElement<GetCustomersByName> createGetCustomersByName(GetCustomersByName value) {
        return new JAXBElement<GetCustomersByName>(_GetCustomersByName_QNAME, GetCustomersByName.class, null, value);
    }

    @XmlElementDecl(namespace = "http://multipart.it.soap.component.quarkus.camel.apache.org/", name = "getCustomersByNameResponse")
    public JAXBElement<GetCustomersByNameResponse> createGetCustomersByNameResponse(GetCustomersByNameResponse value) {
        return new JAXBElement<GetCustomersByNameResponse>(_GetCustomersByNameResponse_QNAME, GetCustomersByNameResponse.class,
                null, value);
    }

    @XmlElementDecl(namespace = "http://multipart.it.soap.component.quarkus.camel.apache.org/", name = "NoSuchCustomer")
    public JAXBElement<NoSuchCustomer> createNoSuchCustomer(NoSuchCustomer value) {
        return new JAXBElement<NoSuchCustomer>(_NoSuchCustomer_QNAME, NoSuchCustomer.class, null, value);
    }

    @XmlElementDecl(namespace = "http://multipart.it.soap.component.quarkus.camel.apache.org/", name = "getAllCustomers")
    public JAXBElement<Object> createGetAllCustomers(Object value) {
        return new JAXBElement<Object>(_GetAllCustomers_QNAME, Object.class, null, value);
    }

    @XmlElementDecl(namespace = "http://multipart.it.soap.component.quarkus.camel.apache.org/", name = "getAllCustomersResponse")
    public JAXBElement<GetAllCustomersResponse> createGetAllCustomersResponse(GetAllCustomersResponse value) {
        return new JAXBElement<GetAllCustomersResponse>(_GetAllCustomersResponse_QNAME, GetAllCustomersResponse.class, null,
                value);
    }

    @XmlElementDecl(namespace = "http://multipart.it.soap.component.quarkus.camel.apache.org/", name = "saveCustomer")
    public JAXBElement<SaveCustomer> createSaveCustomer(SaveCustomer value) {
        return new JAXBElement<SaveCustomer>(_SaveCustomer_QNAME, SaveCustomer.class, null, value);
    }

    @XmlElementDecl(namespace = "http://multipart.it.soap.component.quarkus.camel.apache.org/", name = "saveCustomerToo")
    public JAXBElement<SaveCustomer> createSaveCustomerToo(SaveCustomer value) {
        return new JAXBElement<SaveCustomer>(_SaveCustomerToo_QNAME, SaveCustomer.class, null, value);
    }

    @XmlElementDecl(namespace = "http://multipart.it.soap.component.quarkus.camel.apache.org/", name = "company")
    public JAXBElement<Company> createCompany(Company value) {
        return new JAXBElement<Company>(_Company_QNAME, Company.class, null, value);
    }

    @XmlElementDecl(namespace = "http://multipart.it.soap.component.quarkus.camel.apache.org/", name = "companyType")
    public JAXBElement<CompanyType> createCompanyType(CompanyType value) {
        return new JAXBElement<CompanyType>(_CompanyType_QNAME, CompanyType.class, null, value);
    }

    @XmlElementDecl(namespace = "http://multipart.it.soap.component.quarkus.camel.apache.org/", name = "product")
    public JAXBElement<Product> createProduct(Product value) {
        return new JAXBElement<Product>(_Product_QNAME, Product.class, null, value);
    }

}
