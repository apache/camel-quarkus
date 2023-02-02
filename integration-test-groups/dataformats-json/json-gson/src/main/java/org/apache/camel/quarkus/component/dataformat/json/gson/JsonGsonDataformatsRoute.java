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
package org.apache.camel.quarkus.component.dataformat.json.gson;

import java.lang.reflect.Type;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.reflect.TypeToken;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.gson.GsonDataFormat;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.quarkus.component.dataformat.json.gson.model.*;
import org.apache.camel.spi.DataFormat;

@ApplicationScoped
public class JsonGsonDataformatsRoute extends RouteBuilder {

    @Override
    public void configure() {
        GsonDataFormat gsonDummyObjectDataFormat = new GsonDataFormat();
        Type genericType = new TypeToken<List<DummyObject>>() {
        }.getType();
        gsonDummyObjectDataFormat.setUnmarshalGenericType(genericType);
        gsonDummyObjectDataFormat.setDateFormatPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        gsonDummyObjectDataFormat.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
        gsonDummyObjectDataFormat.setExclusionStrategies(List.of(new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(FieldAttributes f) {
                return f.getAnnotation(ExcludeField.class) != null;
            }

            @Override
            public boolean shouldSkipClass(Class<?> clazz) {
                return false;
            }
        }));

        GsonDataFormat unmarshalByTypeNameGsonDataFormat = new GsonDataFormat();
        unmarshalByTypeNameGsonDataFormat
                .setUnmarshalTypeName("org.apache.camel.quarkus.component.dataformat.json.gson.model.PojoA");
        configureJsonRoutes(JsonLibrary.Gson, gsonDummyObjectDataFormat, unmarshalByTypeNameGsonDataFormat,
                new GsonDataFormat(PojoB.class));
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

        from("direct:Gson-type-as-attribute")
                .unmarshal().json(library, AnotherObject.class);

        from("direct:Gson-type-as-header")
                .setHeader("CamelGsonUnmarshalType")
                .constant("org.apache.camel.quarkus.component.dataformat.json.gson.model.AnotherObject")
                .unmarshal().json(library);
    }
}
