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

import java.util.Objects;

import io.quarkus.builder.item.MultiBuildItem;

public final class CamelLazyProxyBuildItem extends MultiBuildItem {
    private final String type;
    private final String proxy;

    /**
     * @param type  the Java type of the bean
     * @param proxy the Java type of the proxy
     */
    public CamelLazyProxyBuildItem(String type, String proxy) {
        this.type = Objects.requireNonNull(type);
        this.proxy = Objects.requireNonNull(proxy);
    }

    public String getType() {
        return type;
    }

    public String getProxy() {
        return proxy;
    }
}
