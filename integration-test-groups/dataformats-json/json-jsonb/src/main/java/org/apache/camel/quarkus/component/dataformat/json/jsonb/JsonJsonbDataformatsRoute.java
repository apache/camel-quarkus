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
package org.apache.camel.quarkus.component.dataformat.json.jsonb;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jsonb.JsonbDataFormat;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.quarkus.component.dataformat.json.jsonb.model.AnotherObject;
import org.apache.camel.quarkus.component.dataformat.json.jsonb.model.DummyObject;
import org.apache.camel.quarkus.component.dataformat.json.jsonb.model.PojoA;
import org.apache.camel.quarkus.component.dataformat.json.jsonb.model.PojoB;
import org.apache.camel.spi.DataFormat;

@ApplicationScoped
public class JsonJsonbDataformatsRoute extends RouteBuilder {

    @Override
    public void configure() {
        JsonbDataFormat jsonBDummyObjectDataFormat = new JsonbDataFormat(new ParamType(List.class, DummyObject.class));
        configureJsonRoutes(JsonLibrary.Jsonb, jsonBDummyObjectDataFormat, new JsonbDataFormat(PojoA.class),
                new JsonbDataFormat(PojoB.class));
    }

    public void configureJsonRoutes(JsonLibrary library, DataFormat dummyObjectDataFormat, DataFormat pojoADataFormat,
            DataFormat pojoBDataFormat) {

        fromF("direct:%s-in", library)
                .wireTap("direct:" + library + "-tap")
                .setBody(constant("ok"));

        fromF("direct:%s-tap", library)
                .unmarshal(dummyObjectDataFormat)
                .toF("log:%s-out", library)
                .split(body())
                .marshal(dummyObjectDataFormat)
                .convertBodyTo(String.class)
                .toF("vm:%s-out", library);

        fromF("direct:%s-in-a", library)
                .wireTap("direct:" + library + "-tap-a")
                .setBody(constant("ok"));

        fromF("direct:%s-tap-a", library)
                .unmarshal().json(library, PojoA.class)
                .toF("log:%s-out", library)
                .marshal(pojoADataFormat)
                .convertBodyTo(String.class)
                .toF("vm:%s-out-a", library);

        fromF("direct:%s-in-b", library)
                .wireTap("direct:" + library + "-tap-b")
                .setBody(constant("ok"));

        fromF("direct:%s-tap-b", library)
                .unmarshal().json(library, PojoB.class)
                .toF("log:%s-out", library)
                .marshal(pojoBDataFormat)
                .convertBodyTo(String.class)
                .toF("vm:%s-out-b", library);

        from("direct:Jsonb-type-as-attribute")
                .unmarshal().json(library, AnotherObject.class);

        from("direct:Jsonb-type-as-header")
                .setHeader("CamelJsonbUnmarshalType")
                .constant("org.apache.camel.quarkus.component.dataformat.json.gson.model.AnotherObject")
                .unmarshal().json(library);
    }
}
