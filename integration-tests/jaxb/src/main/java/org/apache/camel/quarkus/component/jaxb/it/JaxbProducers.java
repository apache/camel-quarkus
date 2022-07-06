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

import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.apache.camel.quarkus.component.jaxb.it.model.namespaced.NamespacedPerson;
import org.apache.camel.quarkus.component.jaxb.it.model.partial.PartClassPerson;
import org.apache.camel.quarkus.component.jaxb.it.model.pojo.PojoPerson;
import org.apache.camel.quarkus.component.jaxb.it.model.simple.SimplePerson;
import org.apache.camel.quarkus.component.jaxb.it.writer.CustomXmlStreamWriter;

public class JaxbProducers {

    @Singleton
    @Named("jaxbDefault")
    public JaxbDataFormat defaultJaxbDataFormat() {
        JaxbDataFormat dataFormat = new JaxbDataFormat();
        dataFormat.setContextPath(SimplePerson.class.getPackageName());
        dataFormat.setFragment(true);
        dataFormat.setIgnoreJAXBElement(false);
        dataFormat.setPrettyPrint(false);
        dataFormat.setSchema("classpath:person.xsd");
        dataFormat.setSchemaSeverityLevel(2);
        return dataFormat;
    }

    @Singleton
    @Named("jaxbWithNamespacePrefix")
    public JaxbDataFormat jaxbDataFormatWithNamespacePrefix() {
        JaxbDataFormat dataFormat = new JaxbDataFormat();
        dataFormat.setContextPath(NamespacedPerson.class.getPackageName());
        dataFormat.setNamespacePrefix(Map.of("https://example.com/a", "test"));
        return dataFormat;
    }

    @Singleton
    @Named("jaxbWithEncoding")
    public JaxbDataFormat jaxbDataFormatWithCustomCharset() {
        JaxbDataFormat dataFormat = new JaxbDataFormat();
        dataFormat.setContextPath(SimplePerson.class.getPackageName());
        dataFormat.setEncoding("ISO-8859-1");
        dataFormat.setFilterNonXmlChars(true);
        return dataFormat;
    }

    @Singleton
    @Named("jaxbWithMustBeJAXBElementFalse")
    public JaxbDataFormat jaxbDataFormatWithMustBeJAXBElementFalse() {
        JaxbDataFormat dataFormat = new JaxbDataFormat();
        dataFormat.setMustBeJAXBElement(false);
        return dataFormat;
    }

    @Singleton
    @Named("jaxbWithPartClass")
    public JaxbDataFormat jaxbWithPartClass() {
        JaxbDataFormat dataFormat = new JaxbDataFormat();
        dataFormat.setContextPath(PartClassPerson.class.getPackageName());
        dataFormat.setPartClass(PartClassPerson.class);
        dataFormat.setPartNamespace(new QName(PartClassPerson.NAMESPACE, "person"));
        return dataFormat;
    }

    @Singleton
    @Named("jaxbWithIgnoreElement")
    public JaxbDataFormat jaxbWithIgnoreElement() {
        JaxbDataFormat dataFormat = new JaxbDataFormat();
        dataFormat.setContextPath(PartClassPerson.class.getPackageName());
        dataFormat.setIgnoreJAXBElement(false);
        dataFormat.setPartClass(PartClassPerson.class);
        dataFormat.setPartNamespace(new QName(PartClassPerson.NAMESPACE, "person"));
        return dataFormat;
    }

    @Singleton
    @Named("jaxbWithCustomProperties")
    public JaxbDataFormat jaxbWithCustomProperties() {
        String packages = String.format("%s:%s",
                SimplePerson.class.getPackageName(),
                NamespacedPerson.class.getPackageName());
        JaxbDataFormat dataFormat = new JaxbDataFormat(packages);
        dataFormat.setJaxbProviderProperties(Map.of(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.FALSE));
        return dataFormat;
    }

    @Singleton
    @Named("jaxbWithCustomStreamWriter")
    public JaxbDataFormat jaxbWithCustomStreamWriter() {
        JaxbDataFormat dataFormat = new JaxbDataFormat(SimplePerson.class.getPackageName());
        dataFormat.setXmlStreamWriterWrapper(new CustomXmlStreamWriter());
        return dataFormat;
    }

    @Singleton
    @Named("jaxbWithoutObjectFactory")
    public JaxbDataFormat jaxbWithoutObjectFactory() {
        JaxbDataFormat dataFormat = new JaxbDataFormat(PojoPerson.class.getPackageName());
        dataFormat.setObjectFactory(false);
        return dataFormat;
    }

    @Singleton
    @Named("jaxbWithNoNamespaceSchemaLocation")
    public JaxbDataFormat jaxbWithNoNamespaceSchemaLocation() {
        JaxbDataFormat dataFormat = new JaxbDataFormat(SimplePerson.class.getPackageName());
        dataFormat.setNoNamespaceSchemaLocation("person-no-namespace.xsd");
        return dataFormat;
    }
}
