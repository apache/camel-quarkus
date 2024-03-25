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
package org.apache.camel.quarkus.component.kubernetes.cluster;

import java.util.function.BooleanSupplier;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "camel.cluster.kubernetes", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class KubernetesClusterServiceBuildTimeConfig {
    /**
     * Whether a Kubernetes Cluster Service should be automatically configured
     * according to 'quarkus.camel.cluster.kubernetes.*' configurations.
     *
     * @deprecated this property is no longer needed as the Kubernetes implementation of the Camel CLuster Service API has
     *             been moved to a dedicated extension.
     */
    @Deprecated(since = "3.10.0", forRemoval = true)
    @ConfigItem(defaultValue = "true")
    public boolean enabled;

    /**
     * Whether the camel master namespace leaders should be distributed evenly
     * across all the camel contexts in the cluster.
     */
    @ConfigItem(defaultValue = "true")
    public boolean rebalancing;

    public static final class Enabled implements BooleanSupplier {
        KubernetesClusterServiceBuildTimeConfig config;

        @Override
        public boolean getAsBoolean() {
            return config.enabled;
        }
    }
}
