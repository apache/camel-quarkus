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

import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;
import org.apache.camel.component.kubernetes.cluster.LeaseResourceType;
import org.eclipse.microprofile.config.ConfigProvider;

@ConfigRoot(name = "camel.cluster.kubernetes")
public class KubernetesClusterServiceConfig {

    /**
     * Whether a Kubernetes Cluster Service should be automatically configured
     * according to 'quarkus.camel.cluster.kubernetes.*' configurations.
     */
    @ConfigItem(defaultValue = "false")
    public boolean enabled;

    /**
     * The cluster service ID (defaults to null).
     */
    @ConfigItem
    public Optional<String> id;

    /**
     * The URL of the Kubernetes master (read from Kubernetes client properties by default).
     */
    @ConfigItem
    public Optional<String> masterUrl;

    /**
     * The connection timeout in milliseconds to use when making requests to the Kubernetes API server.
     */
    @ConfigItem
    public Optional<Integer> connectionTimeoutMillis;

    /**
     * The name of the Kubernetes namespace containing the pods and the configmap (autodetected by default).
     */
    @ConfigItem
    public Optional<String> namespace;

    /**
     * The name of the current pod (autodetected from container host name by default).
     */
    @ConfigItem
    public Optional<String> podName;

    /**
     * The jitter factor to apply in order to prevent all pods to call Kubernetes APIs in the same instant (defaults to
     * 1.2).
     */
    @ConfigItem
    public Optional<Double> jitterFactor;

    /**
     * The default duration of the lease for the current leader (defaults to 15000).
     */
    @ConfigItem
    public Optional<Long> leaseDurationMillis;

    /**
     * The deadline after which the leader must stop its services because it may have lost the leadership (defaults to
     * 10000).
     */
    @ConfigItem
    public Optional<Long> renewDeadlineMillis;

    /**
     * The time between two subsequent attempts to check and acquire the leadership. It is randomized using the jitter
     * factor (defaults to 2000).
     */
    @ConfigItem
    public Optional<Long> retryPeriodMillis;

    /**
     * Service lookup order/priority (defaults to 2147482647).
     */
    @ConfigItem
    public Optional<Integer> order;

    /**
     * The name of the lease resource used to do optimistic locking (defaults to
     * 'leaders'). The resource name is used as prefix when the underlying
     * Kubernetes resource can manage a single lock.
     */
    @ConfigItem
    public Optional<String> resourceName;

    /**
     * The lease resource type used in Kubernetes, either 'config-map' or
     * 'lease' (defaults to 'lease').
     */
    @ConfigItem
    public Optional<LeaseResourceType> leaseResourceType;

    /**
     * Whether the camel master namespace leaders should be distributed evenly
     * across all the camel contexts in the cluster.
     */
    @ConfigItem(defaultValue = "true")
    public boolean rebalancing;

    /**
     * The labels key/value used to identify the pods composing the cluster,
     * defaults to empty map.
     */
    @ConfigItem
    public Map<String, String> labels;

    public static final class Enabled implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            return ConfigProvider.getConfig().getOptionalValue("quarkus.camel.cluster.kubernetes.enabled", Boolean.class)
                    .orElse(Boolean.FALSE);
        }
    }

}
