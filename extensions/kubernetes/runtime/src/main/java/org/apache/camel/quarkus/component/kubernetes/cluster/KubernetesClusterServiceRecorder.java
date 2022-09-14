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

    public RuntimeValue<KubernetesClusterService> createKubernetesClusterService(KubernetesClusterServiceConfig config) {
        KubernetesClusterService kcs = setupKubernetesClusterServiceFromConfig(config);
        return new RuntimeValue<KubernetesClusterService>(kcs);
    }

    public RuntimeValue<RebalancingCamelClusterService> createKubernetesRebalancingClusterService(
            KubernetesClusterServiceConfig config) {
        KubernetesClusterService kcs = setupKubernetesClusterServiceFromConfig(config);
        RebalancingCamelClusterService rebalancingService = new RebalancingCamelClusterService(kcs,
                kcs.getRenewDeadlineMillis());
        return new RuntimeValue<RebalancingCamelClusterService>(rebalancingService);
    }

    private KubernetesClusterService setupKubernetesClusterServiceFromConfig(KubernetesClusterServiceConfig config) {
        KubernetesClusterService clusterService = new KubernetesClusterService();

        config.id.ifPresent(id -> clusterService.setId(id));
        config.masterUrl.ifPresent(url -> clusterService.setMasterUrl(url));
        config.connectionTimeoutMillis.ifPresent(ctm -> clusterService.setConnectionTimeoutMillis(ctm));
        config.namespace.ifPresent(ns -> clusterService.setKubernetesNamespace(ns));
        config.podName.ifPresent(pn -> clusterService.setPodName(pn));
        config.jitterFactor.ifPresent(jf -> clusterService.setJitterFactor(jf));
        config.leaseDurationMillis.ifPresent(ldm -> clusterService.setLeaseDurationMillis(ldm));
        config.renewDeadlineMillis.ifPresent(rdm -> clusterService.setRenewDeadlineMillis(rdm));
        config.retryPeriodMillis.ifPresent(rpm -> clusterService.setRetryPeriodMillis(rpm));
        config.order.ifPresent(o -> clusterService.setOrder(o));
        config.resourceName.ifPresent(krn -> clusterService.setKubernetesResourceName(krn));
        config.leaseResourceType.ifPresent(lrt -> clusterService.setLeaseResourceType(lrt));

        clusterService.setClusterLabels(config.labels);

        return clusterService;
    }
}
