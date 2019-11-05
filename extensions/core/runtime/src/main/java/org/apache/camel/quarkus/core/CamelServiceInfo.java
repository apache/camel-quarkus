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
package org.apache.camel.quarkus.core;

import java.nio.file.Path;

/**
 * Utility class to describe a camel service which is a result of reading
 * services from resources belonging to META-INF/services/org/apache/camel.
 */
public class CamelServiceInfo {
    /**
     * The path of the service file like META-INF/services/org/apache/camel/component/file.
     */
    public final Path path;

    /**
     * The name of the service entry which is derived from the service path. As example the
     * name for a service with path <code>META-INF/services/org/apache/camel/component/file</code>
     * will be <code>file</code>
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
    public String toString() {
        return "ServiceInfo{"
                + "path='" + path.toString() + '\''
                + ", name=" + name
                + ", type=" + type
                + '}';
    }
}
