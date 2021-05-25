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

package org.apache.camel.quarkus.component.dataformats.jackson.json;

import java.util.TimeZone;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonConstants;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.component.jackson.ListJacksonDataFormat;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.quarkus.component.dataformats.json.model.DummyObject;
import org.apache.camel.quarkus.component.dataformats.json.model.MyModule;
import org.apache.camel.quarkus.component.dataformats.json.model.Pojo;
import org.apache.camel.quarkus.component.dataformats.json.model.TestPojo;
import org.apache.camel.quarkus.component.dataformats.json.model.TestPojoView;
import org.apache.camel.quarkus.component.dataformats.json.model.Views;

public class JacksonJsonRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        JacksonDataFormat unmarshalTypeHeaderFormat = new JacksonDataFormat();
        unmarshalTypeHeaderFormat.setAllowUnmarshallType(true);
        from("direct:jackson-unmarshal-type-header-backPojo").unmarshal(unmarshalTypeHeaderFormat)
                .to("mock:jackson-unmarshal-type-header-reversePojo");

        JacksonDataFormat listFormat = new JacksonDataFormat(TestPojo.class);
        listFormat.useList();
        from("direct:jackson-unmarshal-backPojo").unmarshal(listFormat).to("mock:jackson-unmarshal-reversePojo");

        JacksonDataFormat listSplitFormat = new JacksonDataFormat(DummyObject.class);
        listSplitFormat.useList();
        from("direct:jackson-unmarshal-listsplit-start").unmarshal(listSplitFormat).split(body())
                .to("mock:jackson-unmarshal-listsplit-result");

        JacksonDataFormat includeDefaultFormat = new JacksonDataFormat();
        from("direct:jackson-marshal-includedefault-marshal").marshal(includeDefaultFormat)
                .to("mock:jackson-marshal-includedefault-marshal");

        from("direct:jackson-unmarshal-beginArray").unmarshal().json(JsonLibrary.Jackson, String[].class)
                .to("mock:jackson-unmarshal-endArray");

        JacksonDataFormat marshalContentTypeformat = new JacksonDataFormat();
        from("direct:jackson-marshal-ct-yes").marshal(marshalContentTypeformat);
        from("direct:jackson-marshal-ct-yes2").marshal().json(JsonLibrary.Jackson);
        JacksonDataFormat formatNoHeader = new JacksonDataFormat();
        formatNoHeader.setContentTypeHeader(false);
        from("direct:jackson-marshal-ct-no").marshal(formatNoHeader);

        JacksonDataFormat format = new JacksonDataFormat();
        from("direct:jackson-marshal-in").marshal(format);
        from("direct:jackson-marshal-back").unmarshal(format).to("mock:jackson-marshal-reverse");
        JacksonDataFormat prettyPrintDataFormat = new JacksonDataFormat();
        prettyPrintDataFormat.setPrettyPrint(true);
        from("direct:jackson-marshal-inPretty").marshal(prettyPrintDataFormat);
        from("direct:jackson-marshal-backPretty").unmarshal(prettyPrintDataFormat).to("mock:jackson-marshal-reverse");
        JacksonDataFormat formatPojo = new JacksonDataFormat(TestPojo.class);
        from("direct:jackson-marshal-inPojo").marshal(formatPojo);
        from("direct:jackson-marshal-backPojo").unmarshal(formatPojo).to("mock:jackson-marshal-reversePojo");

        this.getContext().getRegistry().bind("myMapper", new ObjectMapper());
        JacksonDataFormat objectMapperFormat = new JacksonDataFormat();
        objectMapperFormat.setAutoDiscoverObjectMapper(true);
        from("direct:jackson-objectmapper-in").marshal(objectMapperFormat);
        from("direct:jackson-objectmapper-back").unmarshal(objectMapperFormat).to("mock:jackson-objectmapper-reverse");

        JacksonDataFormat jacksonAllowJmsTypeFormat = new JacksonDataFormat();
        jacksonAllowJmsTypeFormat.setAllowJmsType(true);
        from("direct:jackson-allowjmstype-backPojo").unmarshal(jacksonAllowJmsTypeFormat)
                .to("mock:jackson-allowjmstype-reversePojo");

        JacksonDataFormat jacksonJsonModuleFormat = new JacksonDataFormat();
        jacksonJsonModuleFormat.setInclude("NON_NULL");
        jacksonJsonModuleFormat.setModuleClassNames("org.apache.camel.quarkus.component.dataformats.json.model.MyModule");
        from("direct:jackson-module-marshal").marshal(jacksonJsonModuleFormat).to("mock:jackson-module-marshal");

        JacksonDataFormat jacksonNotUseDefaultMapper = new JacksonDataFormat();
        jacksonNotUseDefaultMapper.setUseDefaultObjectMapper(false);
        from("direct:jackson-not-use-default-mapper-in").marshal(jacksonNotUseDefaultMapper);
        from("direct:jackson-not-use-default-mapper-back").unmarshal(jacksonNotUseDefaultMapper)
                .to("mock:jackson-not-use-default-mapper-reverse");

        ObjectMapper mapper = new ObjectMapper();
        JacksonDataFormat jacksonObjectMapperFormat = new JacksonDataFormat();
        jacksonObjectMapperFormat.setObjectMapper(mapper);

        from("direct:jackson-objectmapper-noreg-in").marshal(jacksonObjectMapperFormat);
        from("direct:jackson-objectmapper-noreg-back").unmarshal(jacksonObjectMapperFormat)
                .to("mock:jackson-objectmapper-noreg-reverse");

        JacksonDataFormat jacksonObjectMapperPrettyPrintDataFormat = new JacksonDataFormat();
        jacksonObjectMapperPrettyPrintDataFormat.setPrettyPrint(true);

        from("direct:jackson-objectmapper-noreg-inPretty").marshal(jacksonObjectMapperPrettyPrintDataFormat);
        from("direct:jackson-objectmapper-noreg-backPretty").unmarshal(jacksonObjectMapperPrettyPrintDataFormat)
                .to("mock:jackson-objectmapper-noreg-reverse");

        JacksonDataFormat jacksonObjectMapperFormatPojo = new JacksonDataFormat(TestPojo.class);

        from("direct:jackson-objectmapper-noreg-inPojo").marshal(jacksonObjectMapperFormatPojo);
        from("direct:jackson-objectmapper-noreg-backPojo").unmarshal(jacksonObjectMapperFormatPojo)
                .to("mock:jackson-objectmapper-noreg-reversePojo");

        from("direct:jackson-pojo-array-beginArray").unmarshal().json(Pojo[].class).to("mock:jackson-pojo-array-endArray");

        from("direct:jackson-concurrent-start").marshal().json(JsonLibrary.Jackson).to("log:marshalled")
                .to("direct:jackson-concurrent-marshalled");
        from("direct:jackson-concurrent-marshalled").unmarshal().json(JsonLibrary.Jackson, TestPojo.class)
                .to("mock:jackson-concurrent-result");

        from("direct:jackson-list-unmarshal-backPojo").unmarshal(new ListJacksonDataFormat(TestPojo.class))
                .to("mock:jackson-list-unmarshal-reversePojo");

        this.getContext().getGlobalOptions().put(JacksonConstants.ENABLE_TYPE_CONVERTER, "true");

        from("direct:jackson-conversion-pojo-test").convertBodyTo(String.class);

        from("direct:jackson-conversion-test").convertBodyTo(TestPojo.class);

        /*java.lang.ClassNotFoundException: com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule
         * in native mode, need to investigate more
         * 
         * JacksonDataFormat jacksonJaxbAnnotationFormat = new JacksonDataFormat();
        from("direct:jackson-jaxb-annotation-in").marshal(jacksonJaxbAnnotationFormat);
        from("direct:jackson-jaxb-annotation-back").unmarshal(jacksonJaxbAnnotationFormat)
                .to("mock:jackson-jaxb-annotation-reverse");
        JacksonDataFormat jacksonJaxbAnnotationFormatPojo = new JacksonDataFormat(TestJAXBPojo.class);
        jacksonJaxbAnnotationFormatPojo.setModuleClassNames(JaxbAnnotationModule.class.getName());
        from("direct:jackson-jaxb-annotation-inPojo").marshal(jacksonJaxbAnnotationFormatPojo);
        from("direct:jackson-jaxb-annotation-backPojo").unmarshal(jacksonJaxbAnnotationFormatPojo)
                .to("mock:jackson-jaxb-annotation-reversePojo");
                
        */

        from("direct:jackson-view-inPojoAgeView").marshal().json(TestPojoView.class, Views.Age.class);
        from("direct:jackson-view-backPojoAgeView").unmarshal().json(JsonLibrary.Jackson, TestPojoView.class)
                .to("mock:jackson-view-reversePojoAgeView");
        from("direct:jackson-view-inPojoWeightView").marshal().json(TestPojoView.class, Views.Weight.class);
        from("direct:jackson-view-backPojoWeightView").unmarshal().json(JsonLibrary.Jackson, TestPojoView.class)
                .to("mock:jackson-view-reversePojoWeightView");

        this.getContext().getRegistry().bind("myJacksonModule", new MyModule());
        JacksonDataFormat jacksonModuleRefFormat = new JacksonDataFormat();
        jacksonModuleRefFormat.setInclude("NON_NULL");
        jacksonModuleRefFormat.setModuleRefs("myJacksonModule");
        from("direct:jackson-module-ref-marshal").marshal(jacksonModuleRefFormat).to("mock:jackson-module-ref-marshal");

        JacksonDataFormat jacksonNotNullFormat = new JacksonDataFormat();
        jacksonNotNullFormat.setInclude("NON_NULL");
        from("direct:jackson-not-null-marshal").marshal(jacksonNotNullFormat).to("mock:jackson-not-null-marshal");

        JacksonDataFormat jacksonTypeHeaderNotAllowedFormat = new JacksonDataFormat();
        from("direct:jackson-typeheader-not-allowed-backPojo").unmarshal(jacksonTypeHeaderNotAllowedFormat)
                .to("mock:jackson-typeheader-not-allowed-reversePojo");

        JacksonDataFormat jacksonTimeZone = new JacksonDataFormat();
        TimeZone timeZone = TimeZone.getTimeZone("Africa/Ouagadougou");
        jacksonTimeZone.setTimezone(timeZone);
        from("direct:jackson-timezone-in").marshal(jacksonTimeZone).to("mock:jackson-timezone-result");
    }

}
