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
package org.apache.camel.quarkus.component.mapstruct;

import java.util.Objects;

import io.quarkus.runtime.RuntimeValue;

/**
 * Holds configuration for dynamic TypeConverter instantiation and registration.
 */
public class ConversionMethodInfo {
    private final Class<?> fromClass;
    private final Class<?> toClass;
    private final boolean cdiBean;
    private final RuntimeValue<?> mapper;
    private final String conversionMethodClassName;

    public ConversionMethodInfo(
            Class<?> fromClass,
            Class<?> toClass,
            boolean cdiBean,
            RuntimeValue<?> mapper,
            String conversionMethodClassName) {
        this.fromClass = fromClass;
        this.toClass = toClass;
        this.cdiBean = cdiBean;
        this.mapper = mapper;
        this.conversionMethodClassName = conversionMethodClassName;
    }

    public Class<?> getFromClass() {
        return fromClass;
    }

    public Class<?> getToClass() {
        return toClass;
    }

    public boolean isCdiBean() {
        return cdiBean;
    }

    public RuntimeValue<?> getMapper() {
        return mapper;
    }

    public String getConversionMethodClassName() {
        return conversionMethodClassName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ConversionMethodInfo that = (ConversionMethodInfo) o;
        return cdiBean == that.cdiBean && Objects.equals(fromClass, that.fromClass) && Objects.equals(toClass, that.toClass)
                && Objects.equals(mapper, that.mapper)
                && Objects.equals(conversionMethodClassName, that.conversionMethodClassName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromClass, toClass, cdiBean, mapper, conversionMethodClassName);
    }
}
