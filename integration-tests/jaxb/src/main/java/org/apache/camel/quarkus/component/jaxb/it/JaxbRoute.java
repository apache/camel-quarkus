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

import javax.xml.bind.JAXBContext;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.apache.camel.quarkus.component.jaxb.it.model.Person;

public class JaxbRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        JaxbDataFormat xml = new JaxbDataFormat();
        JAXBContext context = JAXBContext.newInstance(Person.class);
        xml.setContext(context);
        xml.setNamespacePrefix(Map.of("http://example.com/a", "test"));

        JaxbDataFormat jaxbFromScheme = new JaxbDataFormat();
        jaxbFromScheme.setSchema("classpath:person.xsd");

        from("direct:unmarshal")
                .unmarshal().jaxb("org.apache.camel.quarkus.component.jaxb.it.model");

        from("direct:unmarshal-2")
                .unmarshal(xml);

        from("direct:marshal")
                .marshal(jaxbFromScheme);

        from("direct:marshal-2")
                .marshal(xml);

    }
}
