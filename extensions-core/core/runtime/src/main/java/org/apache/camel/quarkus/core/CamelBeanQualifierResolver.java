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

import java.lang.annotation.Annotation;

/**
 * Abstraction for resolving bean annotation qualifiers.
 */
public interface CamelBeanQualifierResolver {
    /**
     * Resolves bean annotation qualifiers.
     *
     * @return The resolved bean {@link Annotation} qualifiers
     */
    default Annotation[] resolveQualifiers() {
        return null;
    }

    /**
     * Resolves bean annotation qualifiers with the given bean name.
     *
     * @param  beanName The name of the bean
     * @return          The resolved bean {@link Annotation} qualifiers
     */
    default Annotation[] resolveAnnotations(String beanName) {
        return null;
    }

    /**
     * Gets the {@link BeanQualifierResolverIdentifier} associated with this {@link CamelBeanQualifierResolver}.
     *
     * @param  className The class name of the bean
     * @param  beanName  The name of the bean. Can be null
     * @return           The {@link BeanQualifierResolverIdentifier}
     */
    default BeanQualifierResolverIdentifier getIdentifier(String className, String beanName) {
        return BeanQualifierResolverIdentifier.of(className, beanName);
    }
}
