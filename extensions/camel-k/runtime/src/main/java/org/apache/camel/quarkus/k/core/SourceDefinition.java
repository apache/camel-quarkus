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
package org.apache.camel.quarkus.k.core;

import java.util.List;

import org.apache.camel.spi.Configurer;
import org.apache.camel.spi.IdAware;

@Configurer
public class SourceDefinition implements IdAware {
    private String id;
    private String name;
    private String language;
    private String loader;
    private List<String> interceptors;
    // Default as source type
    private SourceType type = SourceType.source;
    private List<String> propertyNames;
    private String location;
    private byte[] content;
    private boolean compressed;

    @Override
    public String getId() {
        return id;
    }

    /**
     * Sets the id
     */
    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    /**
     * The name of the source.
     */
    public void setName(String name) {
        this.name = name;
    }

    public String getLanguage() {
        return language;
    }

    /**
     * The language use to define the source.
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    public String getLoader() {
        return loader;
    }

    /**
     * The {@link SourceLoader} that should be used to load the content of the source.
     */
    public void setLoader(String loader) {
        this.loader = loader;
    }

    public List<String> getInterceptors() {
        return interceptors;
    }

    /**
     * The {@link org.apache.camel.quarkus.SourceLoader.Interceptor} that should be applied.
     */
    public void setInterceptors(List<String> interceptors) {
        this.interceptors = interceptors;
    }

    public SourceType getType() {
        return type;
    }

    /**
     * The {@link SourceType} of the source.
     */
    public void setType(SourceType type) {
        this.type = type;
    }

    public List<String> getPropertyNames() {
        return propertyNames;
    }

    /**
     * The list of properties names the source requires (used only for templates).
     */
    public void setPropertyNames(List<String> propertyNames) {
        this.propertyNames = propertyNames;
    }

    public String getLocation() {
        return location;
    }

    /**
     * The location of the source.
     */
    public void setLocation(String location) {
        this.location = location;
    }

    public byte[] getContent() {
        return content;
    }

    /**
     * The content of the source.
     */
    public void setContent(byte[] content) {
        this.content = content;
    }

    public boolean isCompressed() {
        return compressed;
    }

    /**
     * If the content of the source is compressed.
     */
    public void setCompressed(boolean compressed) {
        this.compressed = compressed;
    }

    @Override
    public String toString() {
        String answer = "";

        if (name != null) {
            answer += "name='" + name + "', ";
        }
        if (language != null) {
            answer += "language='" + language + "', ";
        }
        if (loader != null) {
            answer += "loader='" + loader + "', ";
        }
        if (interceptors != null) {
            answer += "interceptors='" + interceptors + "', ";
        }
        if (type != null) {
            answer += "type='" + type + "', ";
        }
        if (propertyNames != null) {
            answer += "propertyNames='" + propertyNames + "', ";
        }
        if (location != null) {
            answer += "location='" + location + "', ";
        }
        if (compressed) {
            answer += "compressed='true', ";
        }
        if (content != null) {
            answer += "<...>";
        }

        return "SourceDefinition{" + answer + '}';
    }
}
