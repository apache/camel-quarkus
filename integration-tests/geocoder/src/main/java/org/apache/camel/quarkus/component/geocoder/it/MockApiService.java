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
package org.apache.camel.quarkus.component.geocoder.it;

import java.lang.reflect.Field;

import javax.enterprise.context.ApplicationScoped;

import com.google.maps.GeoApiContext;
import io.quarkus.runtime.annotations.RegisterForReflection;

@ApplicationScoped
@RegisterForReflection(targets = GeoApiContext.Builder.class)
public class MockApiService {

    public GeoApiContext createGeoApiContext(String baseUri, String apiKey)
            throws IllegalAccessException, InstantiationException, NoSuchFieldException {
        GeoApiContext.Builder builder = createGeoApiContext(baseUri);
        builder.apiKey(apiKey);
        return builder.build();
    }

    /**
     * Creates a Builder and sets a new baseUrl for mock with reflection, because it is impossible to set it differently!
     *
     * @param  baseUrl
     * @return
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws NoSuchFieldException
     */
    public GeoApiContext.Builder createGeoApiContext(String baseUrl)
            throws IllegalAccessException, InstantiationException, NoSuchFieldException {
        Class<?> clazz = GeoApiContext.Builder.class;
        Object builder = clazz.newInstance();

        Field f1 = builder.getClass().getDeclaredField("baseUrlOverride");
        f1.setAccessible(true);
        f1.set(builder, baseUrl);
        return (GeoApiContext.Builder) builder;
    }
}
