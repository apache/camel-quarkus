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
package org.apache.camel.quarkus.support.retrofit.graal;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "retrofit2.Platform")
final class PlatformSubstitution {
    @Substitute
    static PlatformSubstitution get() {
        return new PlatformSubstitution();
    }

    @Substitute
    boolean isDefaultMethod(Method method) {
        return method.isDefault();
    }

    @Substitute
    Object invokeDefaultMethod(Method method, Class<?> declaringClass, Object object, Object... args) throws Throwable {
        method.setAccessible(true);
        return method.invoke(object, args);
    }

    @Substitute
    List<Object> defaultCallAdapterFactories(Executor callbackExecutor) {
        return Arrays.asList(
                CompletableFutureCallAdapterFactorySubstitution.INSTANCE,
                callbackExecutor != null
                        ? new ExecutorCallAdapterFactorySubstitution(callbackExecutor)
                        : DefaultCallAdapterFactorySubstitution.INSTANCE);
    }

    @Substitute
    int defaultCallAdapterFactoriesSize() {
        return 2;
    }

    @Substitute
    List<? extends retrofit2.Converter.Factory> defaultConverterFactories() {
        return Collections.singletonList(OptionalConverterFactorySubstitution.INSTANCE);
    }

    @Substitute
    int defaultConverterFactoriesSize() {
        return 1;
    }
}
