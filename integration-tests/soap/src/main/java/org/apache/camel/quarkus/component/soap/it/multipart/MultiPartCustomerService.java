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

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;
import jakarta.xml.bind.annotation.XmlSeeAlso;

@WebService(targetNamespace = "http://multipart.it.soap.component.quarkus.camel.apache.org/", name = "MultiPartCustomerService")
@XmlSeeAlso({ ObjectFactory.class })
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
public interface MultiPartCustomerService {

    @WebMethod(action = "http://multipart.it.soap.component.quarkus.camel.apache.org/saveCustomerToo")
    public void saveCustomerToo(

            @WebParam(partName = "parameters", name = "saveCustomerToo", targetNamespace = "http://multipart.it.soap.component.quarkus.camel.apache.org/") SaveCustomer parameters,
            @WebParam(partName = "product", name = "product", targetNamespace = "http://multipart.it.soap.component.quarkus.camel.apache.org/", header = true) Product product,
            @WebParam(partName = "company", mode = WebParam.Mode.INOUT, name = "company", targetNamespace = "http://multipart.it.soap.component.quarkus.camel.apache.org/", header = true) jakarta.xml.ws.Holder<Company> company);

    @WebMethod(action = "http://multipart.it.soap.component.quarkus.camel.apache.org/saveCustomer")
    public void saveCustomer(

            @WebParam(partName = "parameters", name = "saveCustomer", targetNamespace = "http://multipart.it.soap.component.quarkus.camel.apache.org/") SaveCustomer parameters,
            @WebParam(partName = "product", name = "product", targetNamespace = "http://multipart.it.soap.component.quarkus.camel.apache.org/", header = true) Product product,
            @WebParam(partName = "company", mode = WebParam.Mode.INOUT, name = "company", targetNamespace = "http://multipart.it.soap.component.quarkus.camel.apache.org/", header = true) jakarta.xml.ws.Holder<Company> company);

    @WebMethod(action = "http://multipart.it.soap.component.quarkus.camel.apache.org/getCustomersByName")
    @WebResult(name = "getCustomersByNameResponse", targetNamespace = "http://multipart.it.soap.component.quarkus.camel.apache.org/", partName = "parameters")
    public GetCustomersByNameResponse getCustomersByName(

            @WebParam(partName = "parameters", name = "getCustomersByName", targetNamespace = "http://multipart.it.soap.component.quarkus.camel.apache.org/") GetCustomersByName parameters,
            @WebParam(partName = "product", name = "product", targetNamespace = "http://multipart.it.soap.component.quarkus.camel.apache.org/", header = true) Product product)
            throws NoSuchCustomerException;

    @WebMethod(action = "http://multipart.it.soap.component.quarkus.camel.apache.org/getAllCustomers")
    public void getAllCustomers(

            @WebParam(partName = "parameters", mode = WebParam.Mode.OUT, name = "getAllCustomersResponse", targetNamespace = "http://multipart.it.soap.component.quarkus.camel.apache.org/") jakarta.xml.ws.Holder<GetAllCustomersResponse> parameters,
            @WebParam(partName = "companyType", mode = WebParam.Mode.OUT, name = "companyType", targetNamespace = "http://multipart.it.soap.component.quarkus.camel.apache.org/") jakarta.xml.ws.Holder<CompanyType> companyType);
}
