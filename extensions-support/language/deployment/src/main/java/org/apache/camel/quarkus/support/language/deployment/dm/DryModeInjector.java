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

import java.util.Set;

import org.apache.camel.Component;
import org.apache.camel.spi.Injector;

/**
 * {@code DryModeInjector} is used to replace instantiations of non-accepted components with an instantiation of
 * {@link DryModeComponent} for a dry run. The accepted components are safe to start and stop for a dry run and cannot
 * be replaced with a {@link DryModeComponent}.
 */
class DryModeInjector implements Injector {

    /**
     * Name of components for which a mock component is not needed for the dry run.
     */
    private static final Set<String> ACCEPTED_NAMES = Set.of("org.apache.camel.component.bean.BeanComponent",
            "org.apache.camel.component.beanclass.ClassComponent",
            "org.apache.camel.component.kamelet.KameletComponent");

    private final Injector delegate;

    DryModeInjector(Injector delegate) {
        this.delegate = delegate;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T newInstance(Class<T> type) {
        if (mustBeReplaced(type)) {
            return (T) delegate.newInstance(DryModeComponent.class);
        }
        return delegate.newInstance(type);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T newInstance(Class<T> type, String factoryMethod) {
        if (mustBeReplaced(type)) {
            return (T) delegate.newInstance(DryModeComponent.class);
        }
        return delegate.newInstance(type, factoryMethod);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T newInstance(Class<T> type, boolean postProcessBean) {
        if (mustBeReplaced(type)) {
            return (T) delegate.newInstance(DryModeComponent.class);
        }
        return delegate.newInstance(type, postProcessBean);
    }

    @Override
    public boolean supportsAutoWiring() {
        return delegate.supportsAutoWiring();
    }

    /**
     * Indicates whether the given type must be replaced by a mock component.
     * 
     * @param  type the type to check.
     * @return      {@code true} if it should be replaced, {@code false} otherwise.
     * @param  <T>  the type of the class to check.
     */
    private static <T> boolean mustBeReplaced(Class<T> type) {
        return Component.class.isAssignableFrom(type) && !ACCEPTED_NAMES.contains(type.getName());
    }
}
