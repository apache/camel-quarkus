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
package org.apache.camel.quarkus.component.kubernetes.deployment;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import org.apache.camel.quarkus.component.kubernetes.CamelKubernetesRecorder;
import org.apache.camel.quarkus.core.deployment.spi.CamelRuntimeBeanBuildItem;

class KubernetesProcessor {
    private static final String FEATURE = "camel-kubernetes";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    CamelRuntimeBeanBuildItem configureKubernetesClient(CamelKubernetesRecorder recorder,
            BeanContainerBuildItem beanContainer) {
        // Enable Kubernetes endpoints to use the client configured by the Quarkus kubernetes-client extension
        return new CamelRuntimeBeanBuildItem(
                "kubernetesClient",
                KubernetesClient.class.getName(),
                recorder.getKubernetesClient(beanContainer.getValue()));
    }
}
