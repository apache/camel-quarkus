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
package org.apache.camel.quarkus.component.ibm.cos.deployment;

import com.ibm.cloud.objectstorage.ClientConfiguration;
import com.ibm.cloud.objectstorage.auth.DefaultAWSCredentialsProviderChain;
import com.ibm.cloud.objectstorage.auth.InstanceProfileCredentialsProvider;
import com.ibm.cloud.objectstorage.client.builder.AwsClientBuilder;
import com.ibm.cloud.objectstorage.event.SDKProgressPublisher;
import com.ibm.cloud.objectstorage.regions.AwsRegionProviderChain;
import com.ibm.cloud.objectstorage.regions.InstanceMetadataRegionProvider;
import com.ibm.cloud.objectstorage.retry.PredefinedBackoffStrategies;
import com.ibm.cloud.objectstorage.retry.PredefinedRetryPolicies;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;

class IbmCosProcessor {

    private static final String FEATURE = "camel-ibm-cos";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void runtimeInitialized(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitialized) {
        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem(ClientConfiguration.class.getName()));
        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem(AwsClientBuilder.class.getName()));
        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem(AwsRegionProviderChain.class.getName()));
        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem(InstanceMetadataRegionProvider.class.getName()));
    }

    @BuildStep
    void initializeRandomRelatedClassesAtRuntime(
            BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClasses) {
        runtimeInitializedClasses.produce(new RuntimeInitializedClassBuildItem("org.apache.http.impl.auth.NTLMEngineImpl"));
        runtimeInitializedClasses.produce(new RuntimeInitializedClassBuildItem(PredefinedBackoffStrategies.class.getName()));
        runtimeInitializedClasses.produce(new RuntimeInitializedClassBuildItem(PredefinedRetryPolicies.class.getName()));
        runtimeInitializedClasses
                .produce(new RuntimeInitializedClassBuildItem("com.ibm.cloud.objectstorage.auth.BaseCredentialsFetcher"));
        runtimeInitializedClasses
                .produce(new RuntimeInitializedClassBuildItem(DefaultAWSCredentialsProviderChain.class.getName()));
        runtimeInitializedClasses
                .produce(new RuntimeInitializedClassBuildItem(InstanceProfileCredentialsProvider.class.getName()));
    }

    @BuildStep
    void initializeCleanerRelatedClassesAtRuntime(
            BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClasses) {
        runtimeInitializedClasses.produce(
                new RuntimeInitializedClassBuildItem("com.ibm.cloud.objectstorage.event.SDKProgressPublisher$LazyHolder"));
        runtimeInitializedClasses.produce(new RuntimeInitializedClassBuildItem(SDKProgressPublisher.class.getName()));
    }

}
