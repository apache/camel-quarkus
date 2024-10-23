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
package org.apache.camel.quarkus.component.file.cluster;

import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "camel.cluster.file")
public class FileLockClusterServiceConfig {

    /**
     * Whether a File Lock Cluster Service should be automatically configured according to
     * 'quarkus.camel.cluster.file.++*++' configurations.
     *
     * @deprecated  this property is no longer needed as the FileLock implementation of the Camel CLuster Service API has
     *              been moved to a dedicated extension.
     * @asciidoclet
     */
    @Deprecated(since = "3.10.0", forRemoval = true)
    @ConfigItem(defaultValue = "true")
    public boolean enabled;

    /**
     * The cluster service ID (defaults to null).
     *
     * @asciidoclet
     */
    @ConfigItem
    public Optional<String> id;

    /**
     * The root path (defaults to null).
     *
     * @asciidoclet
     */
    @ConfigItem
    public Optional<String> root;

    /**
     * The service lookup order/priority (defaults to 2147482647).
     *
     * @asciidoclet
     */
    @ConfigItem
    public Optional<Integer> order;

    /**
     * The custom attributes associated to the service (defaults to empty map).
     *
     * @asciidoclet
     */
    @ConfigItem
    public Map<String, String> attributes;

    /**
     * The time to wait before starting to try to acquire lock (defaults to 1000ms).
     *
     * @asciidoclet
     */
    @ConfigItem
    public Optional<String> acquireLockDelay;

    /**
     * The time to wait between attempts to try to acquire lock (defaults to 10000ms).
     *
     * @asciidoclet
     */
    @ConfigItem
    public Optional<String> acquireLockInterval;

    public static final class Enabled implements BooleanSupplier {

        FileLockClusterServiceConfig config;

        @Override
        public boolean getAsBoolean() {
            return config.enabled;
        }
    }
}
