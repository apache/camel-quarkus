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
package org.apache.camel.quarkus.component.salesforce.model;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.component.salesforce.api.dto.GlobalObjects;

public class GlobalObjectsAndHeaders {
    private GlobalObjects globalObjects;
    private Map<String, String> headers;

    public GlobalObjectsAndHeaders() {
    }

    public GlobalObjectsAndHeaders(GlobalObjects globalObjects) {
        this.globalObjects = globalObjects;
    }

    public GlobalObjects getGlobalObjects() {
        return globalObjects;
    }

    public void setGlobalObjects(GlobalObjects globalObjects) {
        this.globalObjects = globalObjects;
    }

    public GlobalObjectsAndHeaders withHeader(String key, String value) {
        if (headers == null) {
            headers = new HashMap<>();
        }
        headers.put(key, value);
        return this;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String getHeader(String key) {
        return headers != null ? headers.get(key) : null;
    }
}
