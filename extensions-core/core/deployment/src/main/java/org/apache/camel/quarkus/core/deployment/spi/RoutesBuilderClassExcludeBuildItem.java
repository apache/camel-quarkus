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
import org.apache.camel.quarkus.core.CamelConfig.RoutesDiscoveryConfig;

/**
 * A {@link MultiBuildItem} holding patterns whose matching classes will be excluded from the set of classes from which
 * routes will be instantiated. This is a programmatic way of doing the same thing as can be done via
 * {@link RoutesDiscoveryConfig#excludePatterns}.
 */
public final class RoutesBuilderClassExcludeBuildItem extends MultiBuildItem {
    private final String pattern;

    /**
     * @param  cl a class to exclude
     * @return    a new {@link RoutesBuilderClassExcludeBuildItem}
     */
    public static RoutesBuilderClassExcludeBuildItem ofClass(Class<?> cl) {
        return ofClassName(cl.getName());
    }

    /**
     * @param  className a class name to exclude
     * @return           a new {@link RoutesBuilderClassExcludeBuildItem}
     */
    public static RoutesBuilderClassExcludeBuildItem ofClassName(String className) {
        return new RoutesBuilderClassExcludeBuildItem(className.replace('.', '/'));
    }

    /**
     * @param  pattern a single pattern to exclude like in {@link RoutesDiscoveryConfig#excludePatterns}; should contain
     *                 slashes instead of periods; no leading slash, e.g. {@code com/mycompany/bar/*}
     * @return         a new {@link RoutesBuilderClassExcludeBuildItem}
     */
    public static RoutesBuilderClassExcludeBuildItem ofPathPattern(String pattern) {
        return new RoutesBuilderClassExcludeBuildItem(pattern);
    }

    RoutesBuilderClassExcludeBuildItem(String pattern) {
        this.pattern = pattern;
    }

    public String getPattern() {
        return pattern;
    }

    @Override
    public int hashCode() {
        return pattern.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RoutesBuilderClassExcludeBuildItem other = (RoutesBuilderClassExcludeBuildItem) obj;
        if (pattern == null) {
            if (other.pattern != null)
                return false;
        } else if (!pattern.equals(other.pattern))
            return false;
        return true;
    }

}
