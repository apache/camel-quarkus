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
package org.apache.camel.quarkus.bean;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.apache.camel.component.bean.BeanAnnotationExpressionFactory;
import org.apache.camel.support.ObjectHelper;

/**
 * This alias works around an issue with the bean annotation from camel. In
 * camel, BeanAnnotationExpressionFactory.getFromAnnotation(...) uses the
 * underlying annotation class proxy which is a JVM implementation detail. So,
 * this alias makes it possible to get the bean annotation value in native
 * mode.
 */
@TargetClass(BeanAnnotationExpressionFactory.class)
public final class Target_BeanAnnotationExpressionFactory {
    @Substitute
    protected String getFromAnnotation(Annotation annotation, String attribute) {
        try {
            Method method = annotation.annotationType().getDeclaredMethod(attribute);
            Object value = ObjectHelper.invokeMethod(method, annotation);
            if (value == null) {
                throw new IllegalArgumentException("Cannot determine the " + attribute + " from the annotation: " + annotation);
            }
            return value.toString();
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                    "Cannot determine the " + attribute
                            + " of the annotation: " + annotation + " as it does not have a " + attribute
                            + "() method");
        }
    }
}
