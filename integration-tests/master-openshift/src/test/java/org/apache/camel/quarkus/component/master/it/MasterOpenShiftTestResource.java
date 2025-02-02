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
package org.apache.camel.quarkus.component.master.it;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.PodListBuilder;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.fabric8.kubernetes.api.model.PodStatusBuilder;
import io.quarkus.test.kubernetes.client.KubernetesServerTestResource;

public class MasterOpenShiftTestResource extends KubernetesServerTestResource {
    @Override
    protected void configureServer() {
        super.configureServer();

        PodStatus ready = new PodStatusBuilder().withPhase("Running").addNewCondition().withType("Ready").withStatus("true")
                .endCondition().build();
        PodStatus failed = new PodStatusBuilder().withPhase("Failed").build();
        PodStatus notReady = new PodStatusBuilder().withPhase("Running").addNewCondition().withType("Ready").withStatus("false")
                .endCondition().build();

        final Pod leader = new PodBuilder()
                .withNewMetadata()
                .withName("leader")
                .withNamespace("test")
                .endMetadata()
                .withStatus(ready)
                .build();
        final Pod follower = new PodBuilder()
                .withNewMetadata()
                .withName("follower")
                .withNamespace("test")
                .endMetadata()
                .withStatus(ready)
                .build();

        final Pod badPod1 = new PodBuilder()
                .withNewMetadata()
                .withName("badpod1")
                .withNamespace("test")
                .endMetadata()
                .withStatus(failed)
                .build();

        final Pod badPod2 = new PodBuilder()
                .withNewMetadata()
                .withName("badpod2")
                .withNamespace("test")
                .endMetadata()
                .withStatus(notReady)
                .build();

        PodList podList = new PodListBuilder()
                .withNewMetadata()
                .withResourceVersion("1")
                .endMetadata()
                .withItems(leader, follower, badPod1, badPod2)
                .build();

        server.expect()
                .get()
                .withPath("/api/v1/namespaces/test/pods")
                .andReturn(200, podList).always();
    }
}
