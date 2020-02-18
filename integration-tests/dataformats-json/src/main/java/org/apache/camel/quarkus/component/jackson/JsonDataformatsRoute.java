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
package org.apache.camel.quarkus.component.jackson;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.quarkus.component.jackson.model.DummyObject;
import org.apache.camel.quarkus.component.jackson.model.PojoA;
import org.apache.camel.quarkus.component.jackson.model.PojoB;

public class JsonDataformatsRoute extends RouteBuilder {

    @Override
    public void configure() {
        JacksonDataFormat format = new JacksonDataFormat(DummyObject.class);
        format.useList();

        from("direct:jackson-in")
                .wireTap("direct:jackson-tap")
                .setBody(constant("ok"));
        from("direct:jackson-tap")
                .unmarshal(format)
                .to("log:jackson-out")
                .split(body())
                .marshal(format)
                .convertBodyTo(String.class)
                .to("vm:jackson-out");
        from("direct:jackson-in-a")
                .wireTap("direct:jackson-tap-a")
                .setBody(constant("ok"));
        from("direct:jackson-tap-a")
                .unmarshal().json(JsonLibrary.Jackson, PojoA.class)
                .to("log:jackson-out")
                .marshal(new JacksonDataFormat(PojoA.class))
                .convertBodyTo(String.class)
                .to("vm:jackson-out-a");
        from("direct:jackson-in-b")
                .wireTap("direct:jackson-tap-b")
                .setBody(constant("ok"));
        from("direct:jackson-tap-b")
                .unmarshal().json(JsonLibrary.Jackson, PojoB.class)
                .to("log:jackson-out")
                .marshal(new JacksonDataFormat(PojoB.class))
                .convertBodyTo(String.class)
                .to("vm:jackson-out-b");
    }
}
