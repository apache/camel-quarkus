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
package org.apache.camel.quarkus.component.soap.it;

import javax.inject.Named;
import javax.xml.namespace.QName;

import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.soap.SoapDataFormat;
import org.apache.camel.dataformat.soap.name.QNameStrategy;
import org.apache.camel.dataformat.soap.name.ServiceInterfaceStrategy;
import org.apache.camel.dataformat.soap.name.TypeNameStrategy;
import org.apache.camel.quarkus.component.soap.it.multipart.MultiPartCustomerService;
import org.apache.camel.quarkus.component.soap.it.service.CustomerService;
import org.apache.camel.quarkus.component.soap.it.service.GetCustomersByName;
import org.apache.camel.quarkus.component.soap.it.service.NoSuchCustomer;
import org.apache.camel.quarkus.component.soap.it.service.NoSuchCustomerException;

public class SoapRoutes extends RouteBuilder {

    private static final String SERVICE_CUSTOMERS_BY_NAME_PACKAGE = GetCustomersByName.class.getPackage().getName();
    private static final String MULTIPART_CUSTOMERS_BY_NAME_PACKAGE = org.apache.camel.quarkus.component.soap.it.multipart.GetCustomersByName.class
            .getPackage().getName();

    @Override
    public void configure() {
        from("direct:marshal-soap1.1")
                .marshal("soapDataFormat");

        from("direct:unmarshal-soap1.1")
                .unmarshal("soapDataFormat");

        from("direct:marshal-fault-soap1.1")
                .onException(Exception.class)
                .handled(true)
                .marshal("soapDataFormat")
                .end()
                .process(createSoapFaultProcessor());

        from("direct:unmarshal-fault-soap1.1")
                .unmarshal("soapDataFormat");

        from("direct:unmarshalServiceInterfaceStrategy")
                .unmarshal("soapDataFormatWithServiceInterfaceStrategy");

        from("direct:marshalServiceInterfaceStrategy")
                .marshal("soapDataFormatWithServiceInterfaceStrategy");

        from("direct:marshalQnameStrategy")
                .marshal().soap(SERVICE_CUSTOMERS_BY_NAME_PACKAGE, new QNameStrategy(
                        new QName("http://service.it.soap.component.quarkus.camel.apache.org/", "getCustomersByName")));

        from("direct:unmarshalQnameStrategy")
                .unmarshal().soap(SERVICE_CUSTOMERS_BY_NAME_PACKAGE, new QNameStrategy(
                        new QName("http://service.it.soap.component.quarkus.camel.apache.org/", "getCustomersByName")));

        from("direct:marshal-soap1.2")
                .marshal().soap12(SERVICE_CUSTOMERS_BY_NAME_PACKAGE);

        from("direct:unmarshal-soap1.2")
                .unmarshal().soap12(SERVICE_CUSTOMERS_BY_NAME_PACKAGE);

        from("direct:unmarshal-fault-soap1.2")
                .unmarshal().soap12(SERVICE_CUSTOMERS_BY_NAME_PACKAGE);

        from("direct:marshal-fault-soap1.2")
                .onException(Exception.class)
                .handled(true)
                .marshal().soap12(SERVICE_CUSTOMERS_BY_NAME_PACKAGE)
                .end()
                .process(createSoapFaultProcessor());

        from("direct:marshalMultipart")
                .marshal("soapJaxbDataFormatMultipart");

        from("direct:unmarshalMultipart")
                .unmarshal("soapJaxbDataFormatMultipart");
    }

    @Named("soapDataFormat")
    public SoapDataFormat soapJaxbDataFormat() {
        SoapDataFormat soapJaxbDataFormat = new SoapDataFormat(SERVICE_CUSTOMERS_BY_NAME_PACKAGE,
                new TypeNameStrategy());
        soapJaxbDataFormat.setSchema("classpath:/schema/CustomerService.xsd,classpath:/soap.xsd");
        return soapJaxbDataFormat;
    }

    @Named("soapDataFormatWithServiceInterfaceStrategy")
    public SoapDataFormat soapJaxbDataFormatServiceInterfaceStrategy() {
        return new SoapDataFormat(SERVICE_CUSTOMERS_BY_NAME_PACKAGE,
                new ServiceInterfaceStrategy(CustomerService.class, true));
    }

    @Named("soapJaxbDataFormatMultipart")
    public SoapDataFormat soapJaxbDataFormatMultipart() {
        return new SoapDataFormat(MULTIPART_CUSTOMERS_BY_NAME_PACKAGE,
                new ServiceInterfaceStrategy(MultiPartCustomerService.class, true));
    }

    private Processor createSoapFaultProcessor() {
        return e -> {
            GetCustomersByName request = e.getMessage().getBody(GetCustomersByName.class);
            NoSuchCustomer nsc = new NoSuchCustomer();
            nsc.setCustomerId(request.getName());
            throw new NoSuchCustomerException("Specified customer was not found", nsc);
        };
    }
}
