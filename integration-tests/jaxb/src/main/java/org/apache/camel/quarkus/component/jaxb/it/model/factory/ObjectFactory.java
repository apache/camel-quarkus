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
package org.apache.camel.quarkus.component.jaxb.it.model.factory;

import javax.xml.namespace.QName;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlElementDecl;
import jakarta.xml.bind.annotation.XmlRegistry;

@XmlRegistry
public class ObjectFactory {
    public ObjectFactory() {
    }

    public FactoryInstantiatedPerson createFactoryInstantiatedPerson() {
        return new FactoryInstantiatedPerson();
    }

    @XmlElementDecl(namespace = "", name = "person")
    public JAXBElement<FactoryInstantiatedPerson> createPerson(FactoryInstantiatedPerson value) {
        return new JAXBElement<>(new QName(FactoryInstantiatedPerson.NAMESPACE, "person"), FactoryInstantiatedPerson.class,
                null, value);
    }
}
