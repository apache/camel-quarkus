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
package org.apache.camel.quarkus.component.json.path;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import org.apache.camel.support.ObjectHelper;
import org.apache.camel.support.language.DefaultAnnotationExpressionFactory;

/**
 * This alias works around an issue with the JsonPath annotation from camel. In
 * camel, DefaultAnnotationExpressionFactory.getAnnotationObjectValue() uses the
 * underlying annotation class proxy which is a JVM implementation detail. So,
 * this alias makes it possible to get the JsonPath annotation value in native
 * mode.
 */
@TargetClass(DefaultAnnotationExpressionFactory.class)
public final class Target_DefaultAnnotationExpressionFactory {

    @Substitute
    protected Object getAnnotationObjectValue(Annotation annotation, String methodName) {
        try {
            // Gets the method reference from the annotation itself, not from
            // the underlying proxy
            Method method = annotation.annotationType().getDeclaredMethod(methodName);
            Object value = ObjectHelper.invokeMethod(method, annotation);
            return value;
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Cannot determine the Object value of the annotation: " + annotation
                    + " as it does not have the method: " + methodName
                    + "() method", e);
        }
    }
}
