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
package org.apache.camel.quarkus.support.language.deployment.dm;

import org.apache.camel.Component;
import org.apache.camel.spi.Injector;

/**
 * {@code DryModeInjector} is used to replace instantiations of any component with an instantiation of
 * {@link DryModeComponent} for a dry run.
 */
class DryModeInjector implements Injector {

    private final Injector delegate;

    DryModeInjector(Injector delegate) {
        this.delegate = delegate;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T newInstance(Class<T> type) {
        if (Component.class.isAssignableFrom(type)) {
            return (T) delegate.newInstance(DryModeComponent.class);
        }
        return delegate.newInstance(type);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T newInstance(Class<T> type, String factoryMethod) {
        if (Component.class.isAssignableFrom(type)) {
            return (T) delegate.newInstance(DryModeComponent.class);
        }
        return delegate.newInstance(type, factoryMethod);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T newInstance(Class<T> type, boolean postProcessBean) {
        if (Component.class.isAssignableFrom(type)) {
            return (T) delegate.newInstance(DryModeComponent.class);
        }
        return delegate.newInstance(type, postProcessBean);
    }

    @Override
    public boolean supportsAutoWiring() {
        return delegate.supportsAutoWiring();
    }
}
