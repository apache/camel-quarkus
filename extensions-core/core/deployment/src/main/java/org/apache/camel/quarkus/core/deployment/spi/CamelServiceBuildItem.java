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

import java.nio.file.Path;
import java.util.Objects;

import io.quarkus.builder.item.MultiBuildItem;
import org.apache.camel.quarkus.core.deployment.CamelBeanInfo;

/**
 * A {@link MultiBuildItem} holding information about a service defined in a property file somewhere under
 * {@code META-INF/services/org/apache/camel}.
 */
public final class CamelServiceBuildItem extends MultiBuildItem implements CamelBeanInfo {

    public final Path path;
    public final String name;
    public final String type;

    public CamelServiceBuildItem(Path path, String type) {
        this(path, path.getFileName().toString(), type);
    }

    public CamelServiceBuildItem(Path path, String name, String type) {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(type, () -> "type for path " + path);
        this.path = path;
        this.name = name;
        this.type = type;
    }

    /**
     * @return the path of the service file like {@code META-INF/services/org/apache/camel/component/file}.
     */
    public Path getPath() {
        return path;
    }

    /**
     * @return the name under which this service will be registered in the Camel registry.
     *         This name may or may not be the same as the last segment of {@link #path}.
     */
    public String getName() {
        return name;
    }

    /**
     * @return The fully qualified class name of the service.
     */
    public String getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        /* This must be the same as in other implementations of CamelBeanInfo */
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
        /* This must be the same as in other implementations of CamelBeanInfo */
        return Objects.hash(getName(), getType());
    }

}
