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
package org.apache.camel.quarkus.component.milo.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.stack.core.util.BufferUtil;
import org.eclipse.milo.opcua.stack.core.util.NonceUtil;

class MiloProcessor {
    private static final String FEATURE = "camel-milo";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void runtimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClass) {
        // Required due to static declaration of executors & SecureRandom
        runtimeInitializedClass.produce(new RuntimeInitializedClassBuildItem(BufferUtil.class.getName()));
        runtimeInitializedClass.produce(new RuntimeInitializedClassBuildItem(OpcUaClient.class.getName()));
        runtimeInitializedClass.produce(new RuntimeInitializedClassBuildItem(OpcUaServer.class.getName()));
        runtimeInitializedClass.produce(new RuntimeInitializedClassBuildItem(NonceUtil.class.getName()));
        runtimeInitializedClass.produce(
                new RuntimeInitializedClassBuildItem("com.digitalpetri.netty.fsm.ChannelFsmConfigBuilder$SharedExecutor"));
        runtimeInitializedClass.produce(
                new RuntimeInitializedClassBuildItem("com.digitalpetri.netty.fsm.ChannelFsmConfigBuilder$SharedScheduler"));
    }
}
