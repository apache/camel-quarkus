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
package org.apache.camel.quarkus.core.deployment.spi;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.RuntimeValue;
import org.apache.camel.quarkus.core.CamelBeanQualifierResolver;

/**
 * Holds a {@link CamelBeanQualifierResolver} for a specified bean type.
 */
public final class CamelBeanQualifierResolverBuildItem extends MultiBuildItem {
    private final RuntimeValue<CamelBeanQualifierResolver> runtimeValue;
    private final Class<?> beanType;

    public CamelBeanQualifierResolverBuildItem(Class<?> beanType, RuntimeValue<CamelBeanQualifierResolver> runtimeValue) {
        this.beanType = beanType;
        this.runtimeValue = runtimeValue;
    }

    public Class<?> getBeanType() {
        return beanType;
    }

    public String getBeanTypeName() {
        return beanType.getName();
    }

    public RuntimeValue<CamelBeanQualifierResolver> getRuntimeValue() {
        return runtimeValue;
    }
}
