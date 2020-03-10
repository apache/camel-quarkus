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
package org.apache.camel.quarkus.core.deployment;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A {@link MultiBuildItem} holding a list of class names that would otherwise not be allowed to be registered for
 * reflection due to a Camel Quarkus policy.
 */
public final class UnbannedReflectiveBuildItem extends MultiBuildItem {

    final Set<String> classNames;

    public UnbannedReflectiveBuildItem(Collection<String> classNames) {
        this.classNames = new LinkedHashSet<>(classNames);
    }

    public UnbannedReflectiveBuildItem(String... classNames) {
        this.classNames = new LinkedHashSet<>(Arrays.asList(classNames));
    }

    public Set<String> getClassNames() {
        return Collections.unmodifiableSet(classNames);
    }

}
