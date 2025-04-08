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
package org.apache.camel.quarkus.component.beanio.it;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.beanio.BeanIODataFormat;
import org.apache.camel.dataformat.beanio.BeanIOSplitter;
import org.apache.camel.quarkus.component.beanio.it.model.CustomErrorHandler;
import org.apache.camel.spi.DataFormat;

public class BeanioRoutes extends RouteBuilder {
    @Override
    public void configure() throws Exception {
        DataFormat beanIOEmployeeCSV = new BeanIODataFormat("employee-mapping.xml", "employeeCSV");
        DataFormat beanIOEmployeeXML = new BeanIODataFormat("employee-mapping.xml", "employeeXML");
        DataFormat beanIOEmployeeDelimited = new BeanIODataFormat("employee-mapping.xml", "employeeDelimited");
        DataFormat beanIOEmployeeFixedLength = new BeanIODataFormat("employee-mapping.xml", "employeeFixedLength");
        DataFormat beanIOEmployeeAnnotated = new BeanIODataFormat("employee-mapping.xml", "employeeAnnotated");

        BeanIODataFormat beanIOSingleObject = new BeanIODataFormat("single-object-mapping.xml", "keyValueStream");
        beanIOSingleObject.setUnmarshalSingleObject(true);

        BeanIODataFormat beanIOErrorDataFormat = new BeanIODataFormat("employee-mapping.xml", "employeeCSV");
        beanIOErrorDataFormat.setBeanReaderErrorHandler(new CustomErrorHandler());

        BeanIODataFormat beanIOComplexObject = new BeanIODataFormat("complex-mapping.xml", "securityData");

        BeanIOSplitter splitter = new BeanIOSplitter();
        splitter.setMapping("employee-mapping.xml");
        splitter.setStreamName("employeeCSV");

        from("direct:unmarshal")
                .choice()
                .when(simple("${header.type} == 'ANNOTATED'"))
                .unmarshal(beanIOEmployeeAnnotated)
                .when(simple("${header.type} == 'CSV'"))
                .unmarshal(beanIOEmployeeCSV)
                .when(simple("${header.type} == 'DELIMITED'"))
                .unmarshal(beanIOEmployeeDelimited)
                .when(simple("${header.type} == 'FIXEDLENGTH'"))
                .unmarshal(beanIOEmployeeFixedLength)
                .when(simple("${header.type} == 'XML'"))
                .unmarshal(beanIOEmployeeXML)
                .split(body())
                .setBody().simple("${body}");

        from("direct:marshal")
                .choice()
                .when(simple("${header.type} == 'ANNOTATED'"))
                .marshal(beanIOEmployeeAnnotated)
                .when(simple("${header.type} == 'CSV'"))
                .marshal(beanIOEmployeeCSV)
                .when(simple("${header.type} == 'DELIMITED'"))
                .marshal(beanIOEmployeeDelimited)
                .when(simple("${header.type} == 'FIXEDLENGTH'"))
                .marshal(beanIOEmployeeFixedLength)
                .when(simple("${header.type} == 'XML'"))
                .marshal(beanIOEmployeeXML);

        from("direct:unmarshalSingleObject")
                .unmarshal(beanIOSingleObject);

        from("direct:marshalSingleObject")
                .marshal(beanIOSingleObject);

        from("direct:unmarshalWithErrorHandler")
                .unmarshal(beanIOErrorDataFormat);

        from("direct:marshalComplexObject")
                .marshal(beanIOComplexObject);

        from("direct:unmarshalComplexObject")
                .unmarshal(beanIOComplexObject)
                .split(body())
                .setBody().simple("${body}");

        from("direct:unmarshalWithSplitter")
                .split(splitter).streaming()
                .to("mock:splitEmployees");

        from("direct:unmarshalGlobal")
                .to("dataformat:beanio:unmarshal");

    }
}
