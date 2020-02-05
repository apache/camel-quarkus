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
package org.apache.camel.quarkus.component.gson.it;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.reflect.TypeToken;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.gson.GsonDataFormat;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.quarkus.component.gson.it.model.AdvancedOrder;
import org.apache.camel.quarkus.component.gson.it.model.Order;

public class GsonTestRoute extends RouteBuilder {

    @Override
    public void configure() {
        from("direct:gsonMarshal").marshal().json(JsonLibrary.Gson, Order.class);
        from("direct:gsonUnmarshal").unmarshal().json(JsonLibrary.Gson, Order.class);

        GsonDataFormat advancedGsonDataFormat = new GsonDataFormat();
        advancedGsonDataFormat.setUnmarshalType(AdvancedOrder.class);
        advancedGsonDataFormat.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
        advancedGsonDataFormat.setExclusionStrategies(Arrays.<ExclusionStrategy> asList(new FieldExclusionStrategy()));
        from("direct:gsonMarshalAdvanced").marshal(advancedGsonDataFormat);
        from("direct:gsonUnmarshalAdvanced").unmarshal(advancedGsonDataFormat);

        GsonDataFormat genericTypeGsonDataFormat = new GsonDataFormat();
        Type genericType = new TypeToken<List<Order>>() {
        }.getType();
        genericTypeGsonDataFormat.setUnmarshalGenericType(genericType);
        from("direct:gsonMarshalGenericType").marshal(genericTypeGsonDataFormat);
        from("direct:gsonUnmarshalGenericType").unmarshal(genericTypeGsonDataFormat);
    }

    protected static class FieldExclusionStrategy implements ExclusionStrategy {

        @Override
        public boolean shouldSkipField(FieldAttributes f) {
            return f.getAnnotation(ExcludeField.class) != null;
        }

        @Override
        public boolean shouldSkipClass(Class<?> clazz) {
            return false;
        }
    }
}
