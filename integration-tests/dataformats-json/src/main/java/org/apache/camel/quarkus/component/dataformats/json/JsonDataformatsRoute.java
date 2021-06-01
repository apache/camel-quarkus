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
package org.apache.camel.quarkus.component.dataformats.json;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.reflect.TypeToken;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.gson.GsonDataFormat;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.component.johnzon.JohnzonDataFormat;
import org.apache.camel.component.jsonb.JsonbDataFormat;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.quarkus.component.dataformats.json.model.DummyObject;
import org.apache.camel.quarkus.component.dataformats.json.model.ExcludeField;
import org.apache.camel.quarkus.component.dataformats.json.model.PojoA;
import org.apache.camel.quarkus.component.dataformats.json.model.PojoB;
import org.apache.camel.spi.DataFormat;
import org.apache.johnzon.mapper.reflection.JohnzonParameterizedType;

@ApplicationScoped
public class JsonDataformatsRoute extends RouteBuilder {

    @Inject
    ObjectMapper jacksonObjectMapper;

    @Override
    public void configure() {
        JacksonDataFormat jacksonDummyObjectDataFormat = new JacksonDataFormat(DummyObject.class);
        jacksonDummyObjectDataFormat.useList();
        jacksonDummyObjectDataFormat.setObjectMapper(jacksonObjectMapper);
        configureJsonRoutes(JsonLibrary.Jackson, jacksonDummyObjectDataFormat, new JacksonDataFormat(PojoA.class),
                new JacksonDataFormat(PojoB.class));

        JohnzonDataFormat johnzonDummyObjectDataFormat = new JohnzonDataFormat();
        johnzonDummyObjectDataFormat.setParameterizedType(new JohnzonParameterizedType(List.class,
                DummyObject.class));
        configureJsonRoutes(JsonLibrary.Johnzon, johnzonDummyObjectDataFormat, new JohnzonDataFormat(PojoA.class),
                new JohnzonDataFormat(PojoB.class));

        GsonDataFormat gsonDummyObjectDataFormat = new GsonDataFormat();
        Type genericType = new TypeToken<List<DummyObject>>() {
        }.getType();
        gsonDummyObjectDataFormat.setUnmarshalGenericType(genericType);
        gsonDummyObjectDataFormat.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
        gsonDummyObjectDataFormat.setExclusionStrategies(Arrays.<ExclusionStrategy> asList(new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(FieldAttributes f) {
                return f.getAnnotation(ExcludeField.class) != null;
            }

            @Override
            public boolean shouldSkipClass(Class<?> clazz) {
                return false;
            }
        }));
        configureJsonRoutes(JsonLibrary.Gson, gsonDummyObjectDataFormat, new GsonDataFormat(PojoA.class),
                new GsonDataFormat(PojoB.class));

        JsonbDataFormat jsonBDummyObjectDataFormat = new JsonbDataFormat(new ParamType(List.class, DummyObject.class));
        configureJsonRoutes(JsonLibrary.Jsonb, jsonBDummyObjectDataFormat, new JsonbDataFormat(PojoA.class),
                new JsonbDataFormat(PojoB.class));

        from("direct:jacksonxml-marshal")
                .marshal()
                .jacksonxml(true);

        from("direct:jacksonxml-unmarshal")
                .unmarshal()
                .jacksonxml(PojoA.class);

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
    }
}
