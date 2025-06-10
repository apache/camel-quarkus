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
package org.apache.camel.quarkus.core;

import java.util.Objects;

public final class BeanQualifierResolverIdentifier {
    private final String className;
    private final String beanName;

    BeanQualifierResolverIdentifier(String className, String beanName) {
        this.className = className;
        this.beanName = beanName;
    }

    public static BeanQualifierResolverIdentifier of(String className) {
        return new BeanQualifierResolverIdentifier(className, null);
    }

    public static BeanQualifierResolverIdentifier of(String className, String beanName) {
        return new BeanQualifierResolverIdentifier(className, beanName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        BeanQualifierResolverIdentifier that = (BeanQualifierResolverIdentifier) o;
        return Objects.equals(className, that.className) && Objects.equals(beanName, that.beanName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, beanName);
    }

    @Override
    public String toString() {
        String result = "class name " + className;
        if (beanName != null) {
            result += ", bean name " + beanName;
        }
        return result;
    }
}
