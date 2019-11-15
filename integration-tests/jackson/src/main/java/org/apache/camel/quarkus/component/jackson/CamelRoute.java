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

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;

public class CamelRoute extends RouteBuilder {

    @Override
    public void configure() {
        JacksonDataFormat format = new JacksonDataFormat(DummyObject.class);
        format.useList();
        from("direct:in")
                .wireTap("direct:tap")
                .setBody(constant("ok"));
        from("direct:tap")
                .unmarshal(format)
                .to("log:out")
                .split(body())
                .marshal(format)
                .convertBodyTo(String.class)
                .to("vm:out");
    }

    @RegisterForReflection
    public static class DummyObject {

        private String dummy;

        public DummyObject() {
        }

        public String getDummy() {
            return dummy;
        }

        public void setDummy(String dummy) {
            this.dummy = dummy;
        }
    }

}
