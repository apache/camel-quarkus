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
package org.apache.camel.quarkus.k.catalog.model;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class CatalogSupport {

    private CatalogSupport() {
    }

    private static <T> T unmarshall(String json, Class<T> type) {
        try {
            return new ObjectMapper().readValue(json, type);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static CatalogComponentDefinition unmarshallComponent(String json) {
        return unmarshall(json, CatalogComponentDefinition.Container.class).unwrap();
    }

    public static CatalogLanguageDefinition unmarshallLanguage(String json) {
        return unmarshall(json, CatalogLanguageDefinition.Container.class).unwrap();
    }

    public static CatalogDataFormatDefinition unmarshallDataFormat(String json) {
        return unmarshall(json, CatalogDataFormatDefinition.Container.class).unwrap();
    }

    public static CatalogOtherDefinition unmarshallOther(String json) {
        return unmarshall(json, CatalogOtherDefinition.Container.class).unwrap();
    }
}
