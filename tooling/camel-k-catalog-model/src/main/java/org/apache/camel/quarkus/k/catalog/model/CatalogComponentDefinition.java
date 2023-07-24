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

import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.camel.util.StringHelper;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class CatalogComponentDefinition extends CatalogDefinition {
    private String scheme;
    private String alternativeSchemes;
    private String javaType;

    public Stream<String> getSchemes() {
        final String schemeIDs = StringHelper.trimToNull(alternativeSchemes);

        return schemeIDs == null
                ? Stream.of(scheme)
                : Stream.concat(
                        Stream.of(scheme),
                        StringHelper.splitAsStream(schemeIDs, ","));
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public String getAlternativeSchemes() {
        return alternativeSchemes;
    }

    public void setAlternativeSchemes(String alternativeSchemes) {
        this.alternativeSchemes = alternativeSchemes;
    }

    public String getJavaType() {
        return javaType;
    }

    public void setJavaType(String javaType) {
        this.javaType = javaType;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Container {
        private final CatalogComponentDefinition delegate;

        @JsonCreator
        public Container(
                @JsonProperty("component") CatalogComponentDefinition delegate) {
            this.delegate = delegate;
        }

        public CatalogComponentDefinition unwrap() {
            return delegate;
        }
    }
}
