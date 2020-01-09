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
package org.apache.camel.quarkus.support.spring.graal;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URL;
import java.util.Set;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.util.PathMatcher;

final class SpringSubstitutions {
}

@TargetClass(className = "org.springframework.core.DefaultParameterNameDiscoverer")
final class SubstituteDefaultParameterNameDiscoverer {

    @Alias
    public SubstituteDefaultParameterNameDiscoverer() {
        // Discoverers are not meant to be registered on graal
    }
}

@TargetClass(className = "org.springframework.core.KotlinReflectionParameterNameDiscoverer")
@Delete
final class SubstituteKotlinReflectionParameterNameDiscoverer {

}

@TargetClass(className = "org.springframework.core.StandardReflectionParameterNameDiscoverer")
@Delete
final class SubstituteStandardReflectionParameterNameDiscoverer {

}

@TargetClass(className = "org.springframework.core.LocalVariableTableParameterNameDiscoverer")
@Delete
final class SubstituteLocalVariableTableParameterNameDiscoverer {

}

@TargetClass(className = "org.springframework.beans.BeanUtils$KotlinDelegate")
final class SubstituteBeanUtilsKotlinDelegate {

    @Substitute
    public static <T> Constructor<T> findPrimaryConstructor(Class<T> clazz) {
        throw new UnsupportedOperationException("Kotlin is not supported");
    }

    @Substitute
    public static <T> T instantiateClass(Constructor<T> ctor, Object... args)
            throws IllegalAccessException, InvocationTargetException, InstantiationException {
        throw new UnsupportedOperationException("Kotlin is not supported");
    }
}

@TargetClass(className = "org.springframework.core.MethodParameter$KotlinDelegate")
final class SubstituteMethodParameterKotlinDelegate {

    @Substitute
    public static boolean isOptional(MethodParameter param) {
        throw new UnsupportedOperationException("Kotlin is not supported");
    }

    @Substitute
    static private Type getGenericReturnType(Method method) {
        throw new UnsupportedOperationException("Kotlin is not supported");
    }

    @Substitute
    private static Class<?> getReturnType(Method method) {
        throw new UnsupportedOperationException("Kotlin is not supported");
    }
}

@TargetClass(className = "org.springframework.core.io.VfsUtils")
@Delete
final class SubstituteVfsUtils {

}

@TargetClass(className = "org.springframework.core.io.AbstractFileResolvingResource$VfsResourceDelegate")
final class SubstituteVfsResourceDelegate {

    @Substitute
    public static Resource getResource(URL url) throws IOException {
        throw new UnsupportedOperationException("VFS resources are not supported");
    }

    @Substitute
    public static Resource getResource(URI uri) throws IOException {
        throw new UnsupportedOperationException("VFS resources are not supported");
    }
}

@TargetClass(className = "org.springframework.core.io.support.PathMatchingResourcePatternResolver$VfsResourceMatchingDelegate")
final class SubstituteVfsResourceMatchingDelegate {

    @Substitute
    public static Set<Resource> findMatchingResources(
            URL rootDirURL, String locationPattern, PathMatcher pathMatcher) throws IOException {
        throw new UnsupportedOperationException("VFS resources are not supported");
    }
}
