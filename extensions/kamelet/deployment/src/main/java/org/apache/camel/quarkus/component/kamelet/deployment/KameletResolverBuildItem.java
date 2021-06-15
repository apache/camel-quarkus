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
package org.apache.camel.quarkus.component.kamelet.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Build item used by kamelet providers to plug their own way of resolving kamelets giving a name. This could be
 * leveraged by a future camel-quarkus-kamelet-catalog extension to resolve kamelets as they may have a different naming
 * structure or location in the classpath.
 */
public final class KameletResolverBuildItem extends MultiBuildItem {
    private final KameletResolver resolver;

    public KameletResolverBuildItem(KameletResolver resolver) {
        this.resolver = resolver;
    }

    public KameletResolver getResolver() {
        return this.resolver;
    }
}
