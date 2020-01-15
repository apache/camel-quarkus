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

import java.util.function.Function;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A {@link MultiBuildItem} holding a transformer function to be used to transform {@link CamelServiceInfo}s discovered
 * in the classpath. This can be used e.g. for changing the name under which some particular service will be registered
 * in the Camel registry.
 */
public final class CamelServiceInfoTransformerBuildItem extends MultiBuildItem {
    private final Function<CamelServiceInfo, CamelServiceInfo> transformer;

    public CamelServiceInfoTransformerBuildItem(Function<CamelServiceInfo, CamelServiceInfo> mapper) {
        this.transformer = mapper;
    }

    /**
     * @return the transformer function
     */
    public Function<CamelServiceInfo, CamelServiceInfo> getTransformer() {
        return transformer;
    }

}
