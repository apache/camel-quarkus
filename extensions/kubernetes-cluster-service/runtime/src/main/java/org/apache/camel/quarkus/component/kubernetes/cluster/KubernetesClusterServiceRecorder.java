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

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.component.kubernetes.cluster.KubernetesClusterService;
import org.apache.camel.support.cluster.RebalancingCamelClusterService;

@Recorder
public class KubernetesClusterServiceRecorder {

    public RuntimeValue<KubernetesClusterService> createKubernetesClusterService(KubernetesClusterServiceRuntimeConfig config) {
        KubernetesClusterService kcs = setupKubernetesClusterServiceFromConfig(config);
        return new RuntimeValue<KubernetesClusterService>(kcs);
    }

    public RuntimeValue<RebalancingCamelClusterService> createKubernetesRebalancingClusterService(
            KubernetesClusterServiceRuntimeConfig config) {
        KubernetesClusterService kcs = setupKubernetesClusterServiceFromConfig(config);
        RebalancingCamelClusterService rebalancingService = new RebalancingCamelClusterService(kcs,
                kcs.getRenewDeadlineMillis());
        return new RuntimeValue<RebalancingCamelClusterService>(rebalancingService);
    }

    private KubernetesClusterService setupKubernetesClusterServiceFromConfig(KubernetesClusterServiceRuntimeConfig config) {
        KubernetesClusterService clusterService = new KubernetesClusterService();

        config.id.ifPresent(clusterService::setId);
        config.masterUrl.ifPresent(clusterService::setMasterUrl);
        config.connectionTimeoutMillis.ifPresent(clusterService::setConnectionTimeoutMillis);
        config.namespace.ifPresent(clusterService::setKubernetesNamespace);
        config.podName.ifPresent(clusterService::setPodName);
        config.jitterFactor.ifPresent(clusterService::setJitterFactor);
        config.leaseDurationMillis.ifPresent(clusterService::setLeaseDurationMillis);
        config.renewDeadlineMillis.ifPresent(clusterService::setRenewDeadlineMillis);
        config.retryPeriodMillis.ifPresent(clusterService::setRetryPeriodMillis);
        config.order.ifPresent(clusterService::setOrder);
        config.resourceName.ifPresent(clusterService::setKubernetesResourceName);
        config.leaseResourceType.ifPresent(clusterService::setLeaseResourceType);

        clusterService.setClusterLabels(config.labels);

        return clusterService;
    }
}
