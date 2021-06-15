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
package org.apache.camel.quarkus.component.avro.it;

import javax.enterprise.context.ApplicationScoped;

import example.avro.Admin;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.avro.AvroDataFormat;
import org.apache.camel.quarkus.component.avro.BuildTimeAvroDataFormat;

import static org.apache.camel.quarkus.component.avro.it.AvroSchemaLoader.getSchema;

@ApplicationScoped
public class AvroRoute extends RouteBuilder {

    @BuildTimeAvroDataFormat("user.avsc")
    AvroDataFormat buildTimeAvroDataFormat;

    @Override
    public void configure() {

        from("direct:marshalUsingBuildTimeAvroDataFormat").marshal(buildTimeAvroDataFormat);
        from("direct:unmarshalUsingBuildTimeAvroDataFormat").unmarshal(buildTimeAvroDataFormat);

        from("direct:marshalUsingBuildTimeGeneratedClass").marshal().avro(Admin.class);
        from("direct:unmarshalUsingBuildTimeGeneratedClass").unmarshal().avro(Admin.class);

        AvroDataFormat configureTimeAvroDataFormat = new AvroDataFormat(getSchema());
        from("direct:marshalUsingConfigureTimeAvroDataFormat").marshal(configureTimeAvroDataFormat);
        from("direct:unmarshalUsingConfigureTimeAvroDataFormat").unmarshal(configureTimeAvroDataFormat);

        from("direct:marshalUsingAvroDsl").marshal().avro();
        from("direct:unmarshalUsingInstanceClassNameAvroDsl").unmarshal().avro(Value.class.getName());
        from("direct:unmarshalUsingSchemaAvroDsl").unmarshal().avro(Value.SCHEMA$);
    }
}
