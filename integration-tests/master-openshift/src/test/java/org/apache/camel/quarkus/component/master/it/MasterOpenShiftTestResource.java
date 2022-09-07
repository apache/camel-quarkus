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
import io.quarkus.test.kubernetes.client.OpenShiftServerTestResource;

public class MasterOpenShiftTestResource extends OpenShiftServerTestResource {
    @Override
    protected void configureServer() {
        super.configureServer();

        final Pod leader = new PodBuilder()
                .withNewMetadata()
                .withName("leader")
                .withNamespace("test")
                .and()
                .build();
        final Pod follower = new PodBuilder()
                .withNewMetadata()
                .withName("follower")
                .withNamespace("test")
                .and()
                .build();

        PodList podList = new PodListBuilder()
                .withNewMetadata()
                .withResourceVersion("1")
                .endMetadata()
                .withItems(leader, follower)
                .build();

        server.expect()
                .get()
                .withPath("/api/v1/namespaces/test/pods")
                .andReturn(200, podList).always();
    }
}
