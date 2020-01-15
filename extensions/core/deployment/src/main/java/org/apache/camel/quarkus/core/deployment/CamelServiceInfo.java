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

import java.nio.file.Path;
import java.util.Objects;

/**
 * Utility class to describe a camel service which is a result of reading
 * services from resources belonging to META-INF/services/org/apache/camel.
 */
public class CamelServiceInfo implements CamelBeanInfo {
    /**
     * The path of the service file like META-INF/services/org/apache/camel/component/file.
     */
    public final Path path;

    /**
     * The name under which this service will be registered in the Camel registry.
     * This name may or may not be the same as the last segment of {@link #path}.
     */
    public final String name;

    /**
     * The full qualified class name of the service.
     */
    public final String type;

    public CamelServiceInfo(Path path, String type) {
        this(path, path.getFileName().toString(), type);
    }

    public CamelServiceInfo(Path path, String name, String type) {
        this.path = path;
        this.name = name;
        this.type = type;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getType() {
        return this.type;
    }

    /**
     * @param  newName the overriding name
     * @return         a new {@link CamelServiceInfo} having all fields the same as the current {@link CamelServiceInfo}
     *                 except for {@link #name} which is set to the given {@code newName}
     */
    public CamelServiceInfo withName(String newName) {
        return new CamelServiceInfo(path, newName, type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CamelBeanInfo)) {
            return false;
        }
        CamelBeanInfo info = (CamelBeanInfo) o;
        return Objects.equals(getName(), info.getName()) &&
                Objects.equals(getType(), info.getType());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getType());
    }

    @Override
    public String toString() {
        return "ServiceInfo{"
                + "path='" + path.toString() + '\''
                + ", name=" + name
                + ", type=" + type
                + '}';
    }
}
