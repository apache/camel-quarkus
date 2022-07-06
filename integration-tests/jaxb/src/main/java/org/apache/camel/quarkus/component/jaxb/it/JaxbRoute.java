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
package org.apache.camel.quarkus.component.jaxb.it;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.converter.jaxb.JaxbConstants;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.apache.camel.quarkus.component.jaxb.it.model.factory.FactoryInstantiatedPerson;
import org.apache.camel.quarkus.component.jaxb.it.model.partial.PartClassPerson;
import org.apache.camel.quarkus.component.jaxb.it.model.simple.SimplePerson;

@ApplicationScoped
public class JaxbRoute extends RouteBuilder {

    @Inject
    @Named("jaxbDefault")
    JaxbDataFormat defaultJaxbDataFormat;

    @Inject
    @Named("jaxbWithNamespacePrefix")
    JaxbDataFormat jaxbDataFormatWithNamespacePrefix;

    @Inject
    @Named("jaxbWithEncoding")
    JaxbDataFormat jaxbDataFormatWithEncoding;

    @Inject
    @Named("jaxbWithMustBeJAXBElementFalse")
    JaxbDataFormat jaxbDataFormatWithMustBeJAXBElementFalse;

    @Inject
    @Named("jaxbWithPartClass")
    JaxbDataFormat jaxbDataFormatWithPartClass;

    @Inject
    @Named("jaxbWithIgnoreElement")
    JaxbDataFormat jaxbDataFormatIgnoreElement;

    @Inject
    @Named("jaxbWithCustomProperties")
    JaxbDataFormat jaxbDataFormatCustomProperties;

    @Inject
    @Named("jaxbWithCustomStreamWriter")
    JaxbDataFormat jaxbDataFormatCustomStreamWriter;

    @Inject
    @Named("jaxbWithoutObjectFactory")
    JaxbDataFormat jaxbDataFormatWithoutObjectFactory;

    @Inject
    @Named("jaxbWithNoNamespaceSchemaLocation")
    JaxbDataFormat jaxbDataFormatNoNamespaceSchemaLocation;

    @Override
    public void configure() {
        from("direct:marshal")
                .marshal(defaultJaxbDataFormat);

        from("direct:unmarshal")
                .unmarshal(defaultJaxbDataFormat);

        from("direct:marshalJaxbDsl")
                .marshal().jaxb(SimplePerson.class.getPackageName());

        from("direct:unmarshalJaxbDsl")
                .unmarshal().jaxb(SimplePerson.class.getPackageName());

        from("direct:marshalNamespacePrefix")
                .marshal(jaxbDataFormatWithNamespacePrefix);

        from("direct:unmarshalNamespacePrefix")
                .unmarshal(jaxbDataFormatWithNamespacePrefix);

        from("direct:marshalEncoding")
                .marshal(jaxbDataFormatWithEncoding);

        from("direct:unmarshalEncoding")
                .unmarshal(jaxbDataFormatWithEncoding);

        from("direct:marshalWithMustBeJAXBElementFalse")
                .marshal(jaxbDataFormatWithMustBeJAXBElementFalse);

        from("direct:marshalPartClass")
                .marshal(jaxbDataFormatWithPartClass);

        from("direct:marshalPartClassFromHeader")
                .setHeader(JaxbConstants.JAXB_PART_CLASS, constant(PartClassPerson.class.getName()))
                .setHeader(JaxbConstants.JAXB_PART_NAMESPACE, constant(String.format("{%s}person", PartClassPerson.NAMESPACE)))
                .marshal().jaxb(PartClassPerson.class.getPackageName());

        from("direct:unmarshalPartClass")
                .unmarshal(jaxbDataFormatWithPartClass);

        from("direct:unmarshalPartClassFromHeader")
                .setHeader(JaxbConstants.JAXB_PART_CLASS, constant(PartClassPerson.class.getName()))
                .setHeader(JaxbConstants.JAXB_PART_NAMESPACE, constant(String.format("{%s}person", PartClassPerson.NAMESPACE)))
                .unmarshal().jaxb(PartClassPerson.class.getPackageName());

        from("direct:unmarshalIgnoreJaxbElement")
                .unmarshal(jaxbDataFormatIgnoreElement);

        from("direct:marshalCustomProperties")
                .marshal(jaxbDataFormatCustomProperties);

        from("direct:marshalCustomStreamWriter")
                .marshal(jaxbDataFormatCustomStreamWriter);

        from("direct:marshalWithoutObjectFactory")
                .marshal(jaxbDataFormatWithoutObjectFactory);

        from("direct:marshalNoNamespaceSchemaLocation")
                .marshal(jaxbDataFormatNoNamespaceSchemaLocation);

        from("direct:marshalWithObjectFactory")
                .marshal().jaxb(FactoryInstantiatedPerson.class.getPackageName());
    }
}
