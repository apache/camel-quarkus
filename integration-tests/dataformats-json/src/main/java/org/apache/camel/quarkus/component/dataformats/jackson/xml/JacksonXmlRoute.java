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

package org.apache.camel.quarkus.component.dataformats.jackson.xml;

import java.util.TimeZone;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jacksonxml.JacksonXMLConstants;
import org.apache.camel.component.jacksonxml.JacksonXMLDataFormat;
import org.apache.camel.component.jacksonxml.ListJacksonXMLDataFormat;
import org.apache.camel.quarkus.component.dataformats.json.model.DummyObject;
import org.apache.camel.quarkus.component.dataformats.json.model.MyModule;
import org.apache.camel.quarkus.component.dataformats.json.model.TestJAXBPojo;
import org.apache.camel.quarkus.component.dataformats.json.model.TestPojo;
import org.apache.camel.quarkus.component.dataformats.json.model.TestPojoView;
import org.apache.camel.quarkus.component.dataformats.json.model.Views;

public class JacksonXmlRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        JacksonXMLDataFormat unmarshalTypeHeaderFormat = new JacksonXMLDataFormat();
        unmarshalTypeHeaderFormat.setAllowUnmarshallType(true);
        from("direct:jacksonxml-unmarshal-type-header").unmarshal(unmarshalTypeHeaderFormat);

        JacksonXMLDataFormat listFormat = new JacksonXMLDataFormat(TestPojo.class);
        listFormat.useList();
        from("direct:jacksonxml-unmarshal-list").unmarshal(listFormat).to("mock:jacksonxml-unmarshal-list-reversePojo");

        JacksonXMLDataFormat listSplitFormat = new JacksonXMLDataFormat(DummyObject.class);
        listSplitFormat.useList();
        from("direct:jacksonxml-unmarshal-listsplit").unmarshal(listSplitFormat).split(body()).to("mock:listsplit-result");

        JacksonXMLDataFormat format = new JacksonXMLDataFormat();
        from("direct:jacksonxml-marshal-includedefault").marshal(format).to("mock:marshal-includedefalut");

        JacksonXMLDataFormat marshalContentTypeformat = new JacksonXMLDataFormat();
        from("direct:jacksonxml-marshal-ct-yes").marshal(marshalContentTypeformat);
        from("direct:jacksonxml-marshal-ct-yes2").marshal().jacksonxml();
        JacksonXMLDataFormat marshalContentTypeFormatNoHeader = new JacksonXMLDataFormat();
        marshalContentTypeFormatNoHeader.setContentTypeHeader(false);
        from("direct:jacksonxml-marshal-ct-no").marshal(marshalContentTypeFormatNoHeader);

        JacksonXMLDataFormat jacksonMarshalFormat = new JacksonXMLDataFormat();
        from("direct:jacksonxml-marshal-in").marshal(jacksonMarshalFormat);
        from("direct:jacksonxml-marshal-back").unmarshal(jacksonMarshalFormat).to("mock:reverse");
        JacksonXMLDataFormat prettyPrintDataFormat = new JacksonXMLDataFormat();
        prettyPrintDataFormat.setPrettyPrint(true);
        from("direct:jacksonxml-marshal-inPretty").marshal(prettyPrintDataFormat);
        from("direct:jacksonxml-marshal-backPretty").unmarshal(prettyPrintDataFormat).to("mock:reverse");
        JacksonXMLDataFormat formatPojo = new JacksonXMLDataFormat(TestPojo.class);
        from("direct:jacksonxml-marshal-inPojo").marshal(formatPojo);
        from("direct:jacksonxml-marshal-backPojo").unmarshal(formatPojo).to("mock:reversePojo");

        JacksonXMLDataFormat jacksonXmlMarshalAllowJmsTypeformat = new JacksonXMLDataFormat();
        jacksonXmlMarshalAllowJmsTypeformat.setAllowJmsType(true);
        from("direct:jacksonxml-marshal-allowjmstype-backPojo").unmarshal(jacksonXmlMarshalAllowJmsTypeformat)
                .to("mock:allowjmstype-reversePojo");

        JacksonXMLDataFormat jacksonXmlMarshalModuleformat = new JacksonXMLDataFormat();
        jacksonXmlMarshalModuleformat.setInclude("NON_NULL");
        jacksonXmlMarshalModuleformat.setModuleClassNames("org.apache.camel.quarkus.component.dataformats.json.model.MyModule");
        from("direct:jacksonxml-marshal-module").marshal(jacksonXmlMarshalModuleformat).to("mock:jacksonxml-marshal-module");

        from("direct:jacksonxml-marshal-concurrent-start").marshal().jacksonxml().to("log:marshalled")
                .to("direct:jacksonxml-marshal-concurrent-marshalled");
        from("direct:jacksonxml-marshal-concurrent-marshalled").unmarshal().jacksonxml(TestPojo.class)
                .to("mock:jacksonxml-marshal-concurrent-result");
        this.getContext().getGlobalOptions().put(JacksonXMLConstants.ENABLE_TYPE_CONVERTER, "true");
        from("direct:jacksonxml-marshal-conversion").convertBodyTo(TestPojo.class);

        from("direct:jacksonxml-unmarshal-listjackson").unmarshal(new ListJacksonXMLDataFormat(TestPojo.class))
                .to("mock:jacksonxml-unmarshal-listjackson");

        JacksonXMLDataFormat jacksonXmlJaxbAnnotationFormat = new JacksonXMLDataFormat();
        from("direct:jacksonxml-jaxbannotation-in").marshal(jacksonXmlJaxbAnnotationFormat);
        from("direct:jacksonxml-jaxbannotation-back").unmarshal(jacksonXmlJaxbAnnotationFormat)
                .to("mock:jacksonxml-jaxbannotation-reverse");
        JacksonXMLDataFormat jacksonXmlJaxbAnnotationformatPojo = new JacksonXMLDataFormat(TestJAXBPojo.class);
        from("direct:jacksonxml-jaxbannotation-inPojo").marshal(jacksonXmlJaxbAnnotationformatPojo);
        from("direct:jacksonxml-jaxbannotation-backPojo").unmarshal(jacksonXmlJaxbAnnotationformatPojo)
                .to("mock:jacksonxml-jaxbannotation-reversePojo");

        from("direct:jacksonxml-jsonview-inPojoAgeView").marshal().jacksonxml(TestPojoView.class, Views.Age.class);
        from("direct:jacksonxml-jsonview-backPojoAgeView").unmarshal().jacksonxml(TestPojoView.class)
                .to("mock:jacksonxml-jsonview-reversePojoAgeView");
        from("direct:jacksonxml-jsonview-inPojoWeightView").marshal().jacksonxml(TestPojoView.class, Views.Weight.class);
        from("direct:jacksonxml-jsonview-backPojoWeightView").unmarshal().jacksonxml(TestPojoView.class)
                .to("mock:jacksonxml-jsonview-reversePojoWeightView");

        this.getContext().getRegistry().bind("myJacksonModule", new MyModule());
        JacksonXMLDataFormat jacksonXmlModuleRef = new JacksonXMLDataFormat();
        jacksonXmlModuleRef.setInclude("NON_NULL");
        jacksonXmlModuleRef.setModuleRefs("myJacksonModule");
        from("direct:jacksonxml-moduleref-marshal").marshal(jacksonXmlModuleRef).to("mock:jacksonxml-moduleref-marshal");

        JacksonXMLDataFormat jacksomXmlIncludeNotNullFormat = new JacksonXMLDataFormat();
        jacksomXmlIncludeNotNullFormat.setInclude("NON_NULL");
        from("direct:jacksonxml-include-non-null-marshal").marshal(jacksomXmlIncludeNotNullFormat)
                .to("mock:jacksonxml-include-non-null-marshal");

        JacksonXMLDataFormat jacksonXmlTypeHeaderNotAllowedFormat = new JacksonXMLDataFormat();
        from("direct:jacksonxml-typeheader-not-allowed-backPojo").unmarshal(jacksonXmlTypeHeaderNotAllowedFormat)
                .to("mock:jacksonxml-typeheader-not-allowed-reversePojo");

        JacksonXMLDataFormat jacksonXmlDateTimezoneFormat = new JacksonXMLDataFormat();
        TimeZone timeZone = TimeZone.getTimeZone("Africa/Ouagadougou");
        jacksonXmlDateTimezoneFormat.setTimezone(timeZone);

        from("direct:jacksonxml-datatimezone-in").marshal(jacksonXmlDateTimezoneFormat)
                .to("mock:jacksonxml-datatimezone-result");

    }

}
