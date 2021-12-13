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
package org.apache.camel.quarkus.component.protobuf.it;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.protobuf.ProtobufDataFormat;
import org.apache.camel.quarkus.component.protobuf.it.model.AddressBookProtos.Person;

@RegisterForReflection(targets = {
        org.apache.camel.quarkus.component.protobuf.it.model.AddressBookProtos.Person.Builder.class })
public class ProtobufRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("direct:protobuf-marshal")
                .marshal()
                .protobuf(Person.class.getName());

        from("direct:protobuf-unmarshal")
                .unmarshal()
                .protobuf(Person.class.getName());

        ProtobufDataFormat protobufJsonDataFormat = new ProtobufDataFormat(Person.getDefaultInstance(),
                ProtobufDataFormat.CONTENT_TYPE_FORMAT_JSON);

        from("direct:protobuf-marshal-json")
                .marshal(protobufJsonDataFormat)
                .end();

        from("direct:protobuf-unmarshal-json")
                .unmarshal(protobufJsonDataFormat)
                .end();
    }
}
