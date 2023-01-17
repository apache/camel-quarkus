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
package org.apache.camel.quarkus.support.dsl.deployment;

import io.quarkus.builder.item.MultiBuildItem;

public final class DslGeneratedClassBuildItem extends MultiBuildItem {

    final String name;
    final String location;
    final boolean instantiateWithCamelContext;

    public DslGeneratedClassBuildItem(String name, String location) {
        this(name, location, false);
    }

    public DslGeneratedClassBuildItem(String name, String location, boolean instantiateWithCamelContext) {
        this.name = name;
        this.location = location;
        this.instantiateWithCamelContext = instantiateWithCamelContext;
    }

    public String getName() {
        return this.name;
    }

    public String getLocation() {
        return location;
    }

    public boolean isInstantiateWithCamelContext() {
        return instantiateWithCamelContext;
    }
}
