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
package org.apache.camel.quarkus.component.hashicorp.vault.graal;

import java.util.function.BooleanSupplier;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Substitute Jackson2ObjectMapperBuilder.build() method to avoid references to XmlMapper, when jackson-dataformat-xml
 * is not on the runtime classpath.
 */
@TargetClass(className = "org.springframework.http.converter.json.Jackson2ObjectMapperBuilder", onlyWith = JacksonDataformatXmlIsAbsent.class)
final class XmlObjectMapperInitializerSubstitutions {
    @Alias
    private JsonFactory factory;

    @SuppressWarnings("unchecked")
    @Substitute
    public <T extends ObjectMapper> T build() {
        ObjectMapper mapper = (this.factory != null ? new ObjectMapper(this.factory) : new ObjectMapper());
        configure(mapper);
        return (T) mapper;
    }

    @Alias
    public void configure(ObjectMapper objectMapper) {
    }
}

final class JacksonDataformatXmlIsAbsent implements BooleanSupplier {
    @Override
    public boolean getAsBoolean() {
        try {
            Thread.currentThread().getContextClassLoader().loadClass("com.fasterxml.jackson.dataformat.xml.XmlMapper");
            return false;
        } catch (ClassNotFoundException e) {
            return true;
        }
    }
}
