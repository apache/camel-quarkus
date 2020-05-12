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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A {@link MultiBuildItem} holding a collection of path patterns to select files under
 * {@code META-INF/services/org/apache/camel} which define discoverable Camel services.
 */
public final class CamelServicePatternBuildItem extends MultiBuildItem {

    /**
     * Where a Camel service should be further processed
     */
    public enum CamelServiceDestination {
        /** Service marked with {@link #DISCOVERY} should be made discoverable via FactoryFinder mechanism */
        DISCOVERY,
        /** Service marked with {@link #DISCOVERY} should be registered in the Camel registry */
        REGISTRY
    }

    public CamelServicePatternBuildItem(CamelServiceDestination destination, boolean include,
            String... patterns) {
        this(destination, include, Arrays.asList(patterns));
    }

    public CamelServicePatternBuildItem(CamelServiceDestination destination, boolean include,
            Collection<String> patterns) {
        this.destination = destination;
        this.include = include;
        this.patterns = Collections.unmodifiableList(new ArrayList<>(patterns));
    }

    private final CamelServiceDestination destination;

    private final boolean include;

    private final List<String> patterns;

    /**
     * @return a {@link CamelServiceDestination} that says where this service should be further processed. See
     *         {@link CamelServiceDestination} and its members.
     */
    public CamelServiceDestination getDestination() {
        return destination;
    }

    /**
     * @return {@code true} if the {@link #patterns} should be interpreted as includes; otherwise the {@link #patterns}
     *         should be interpreted as excludes
     */
    public boolean isInclude() {
        return include;
    }

    /**
     * @return a {@link List} or Ant-like path patterns. By convention these
     */
    public List<String> getPatterns() {
        return patterns;
    }

}
