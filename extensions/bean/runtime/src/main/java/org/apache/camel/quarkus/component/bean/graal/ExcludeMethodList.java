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
package org.apache.camel.quarkus.component.bean.graal;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This is a workaround for https://github.com/oracle/graal/issues/1971. An instance of lazily initialized list is set
 * to {@code org.apache.camel.component.bean.BeanInfo.EXCLUDED_METHODS}.
 *
 * @see SubstituteBeanInfo
 */
class ExcludeMethodList extends AbstractList<Method> {

    private volatile List<Method> delegate;

    @Override
    public Method get(int index) {
        return delegate().get(index);
    }

    @Override
    public int size() {
        return delegate().size();
    }

    List<Method> delegate() {
        if (delegate == null) {
            synchronized (this) {
                if (delegate == null) {
                    // The logic is taken from the static initializer of BeanInfo
                    delegate = new ArrayList<>();
                    // exclude all java.lang.Object methods as we dont want to invoke them
                    delegate.addAll(Arrays.asList(Object.class.getDeclaredMethods()));
                    // exclude all java.lang.reflect.Proxy methods as we dont want to invoke them
                    delegate.addAll(Arrays.asList(Proxy.class.getDeclaredMethods()));
                    // Remove private methods
                    delegate.removeIf(m -> Modifier.isPrivate(m.getModifiers()));
                    try {
                        // but keep toString as this method is okay
                        delegate.remove(Object.class.getDeclaredMethod("toString"));
                        delegate.remove(Proxy.class.getDeclaredMethod("toString"));
                    } catch (Throwable e) {
                        // ignore
                    }
                }
            }
        }
        return delegate;
    }

}
