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

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.soap.SoapJaxbDataFormat;
import org.apache.camel.dataformat.soap.name.TypeNameStrategy;
import org.apache.camel.quarkus.component.soap.it.example.GetCustomersByName;

public class SoapRoutes extends RouteBuilder {

    @Override
    public void configure() {
        final String soapPackage = GetCustomersByName.class.getPackage().getName();
        SoapJaxbDataFormat soap = new SoapJaxbDataFormat(soapPackage, new TypeNameStrategy());

        from("direct:marshal")
                .marshal(soap);

        from("direct:unmarshal")
                .unmarshal(soap);

        from("direct:marshal-soap12")
                .marshal().soapjaxb12(soapPackage);

        from("direct:unmarshal-soap12")
                .unmarshal().soapjaxb12(soapPackage);
    }
}
