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
package org.apache.camel.quarkus.core.deployment.spi;

import io.quarkus.builder.item.MultiBuildItem;
import org.jboss.jandex.DotName;

/**
 * Announces a bean that another extension registers synthetically. {@code BeanDiscoveryFinishedBuildItem} lists
 * class-based beans only, so a build step that counts beans of a type through it misses the synthetic ones; the
 * extension knowing about such a bean produces one item per bean, naming the type it counts as and, for a bean
 * qualified by a name, that name. A consumer looking for the default bean of a type must skip the named
 * announcements: a named bean does not carry the {@code @Default} qualifier.
 */
public final class AnnouncedSyntheticBeanBuildItem extends MultiBuildItem {

    private final DotName beanType;
    private final String name;

    /** Announces the default, unqualified bean of the given type. */
    public AnnouncedSyntheticBeanBuildItem(Class<?> beanType) {
        this(DotName.createSimple(beanType), null);
    }

    /**
     * @param beanType the type the bean is counted as
     * @param name     the value of the name qualifier of a named bean, {@code null} for the default bean
     */
    public AnnouncedSyntheticBeanBuildItem(Class<?> beanType, String name) {
        this(DotName.createSimple(beanType), name);
    }

    public AnnouncedSyntheticBeanBuildItem(DotName beanType, String name) {
        this.beanType = beanType;
        this.name = name;
    }

    public DotName getBeanType() {
        return beanType;
    }

    /** The name qualifier value of a named bean, {@code null} for the default bean. */
    public String getName() {
        return name;
    }

    /** Whether the announced bean is the default, unqualified bean of its type. */
    public boolean isDefault() {
        return name == null;
    }
}
