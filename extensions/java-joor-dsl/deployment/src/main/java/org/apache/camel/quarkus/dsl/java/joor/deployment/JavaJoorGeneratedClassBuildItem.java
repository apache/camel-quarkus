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
package org.apache.camel.quarkus.dsl.java.joor.deployment;

import io.quarkus.builder.item.MultiBuildItem;

public final class JavaJoorGeneratedClassBuildItem extends MultiBuildItem {

    final String name;
    final String location;
    final byte[] classData;

    public JavaJoorGeneratedClassBuildItem(String name, String location, byte[] classData) {
        this.name = name;
        this.location = location;
        this.classData = classData;
    }

    public String getName() {
        return this.name;
    }

    public byte[] getClassData() {
        return this.classData;
    }

    public String getLocation() {
        return location;
    }
}
