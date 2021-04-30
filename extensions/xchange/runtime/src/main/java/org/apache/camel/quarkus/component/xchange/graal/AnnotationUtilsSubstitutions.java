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
package org.apache.camel.quarkus.component.xchange.graal;

import java.lang.annotation.Annotation;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import si.mazi.rescu.AnnotationUtils;

@TargetClass(AnnotationUtils.class)
public final class AnnotationUtilsSubstitutions {

    // Replaces original impl to avoid reflective actions on proxy classes
    @Substitute
    static <T extends Annotation> String getValueOrNull(Class<T> annotationClass, Annotation ann) {
        if (!annotationClass.isInstance(ann)) {
            return null;
        }

        try {
            return (String) annotationClass.getMethod("value").invoke(ann);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Can't access element 'value' in  " + annotationClass + ". This is probably a bug in rescu.", e);
        }
    }
}
