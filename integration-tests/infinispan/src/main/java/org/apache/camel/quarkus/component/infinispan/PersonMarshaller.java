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
package org.apache.camel.quarkus.component.infinispan;

import java.io.IOException;

import org.apache.camel.quarkus.component.infinispan.common.model.Person;
import org.infinispan.protostream.MessageMarshaller;

public class PersonMarshaller implements MessageMarshaller<Person> {
    public static final String PACKAGE = "person";
    public static final String NAME = "Person";
    public static final String FULL_NAME = PACKAGE + "." + NAME;

    @Override
    public Person readFrom(ProtoStreamReader reader) throws IOException {
        String firstName = reader.readString("firstName");
        String lastName = reader.readString("lastName");
        return new Person(firstName, lastName);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, Person person) throws IOException {
        writer.writeString("firstName", person.getFirstName());
        writer.writeString("lastName", person.getLastName());
    }

    @Override
    public Class<? extends Person> getJavaClass() {
        return Person.class;
    }

    @Override
    public String getTypeName() {
        return FULL_NAME;
    }
}
