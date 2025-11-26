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

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.stream.Stream;

import com.ibm.cloud.objectstorage.ClientConfiguration;
import com.ibm.cloud.objectstorage.auth.DefaultAWSCredentialsProviderChain;
import com.ibm.cloud.objectstorage.auth.InstanceProfileCredentialsProvider;
import com.ibm.cloud.objectstorage.client.builder.AwsClientBuilder;
import com.ibm.cloud.objectstorage.event.SDKProgressPublisher;
import com.ibm.cloud.objectstorage.oauth.Token;
import com.ibm.cloud.objectstorage.regions.AwsRegionProviderChain;
import com.ibm.cloud.objectstorage.regions.InstanceMetadataRegionProvider;
import com.ibm.cloud.objectstorage.retry.PredefinedBackoffStrategies;
import com.ibm.cloud.objectstorage.retry.PredefinedRetryPolicies;
import com.ibm.cloud.objectstorage.services.s3.internal.AWSS3V4Signer;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;

class IbmCosProcessor {

    private static final String FEATURE = "camel-ibm-cos";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void runtimeInitialized(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitialized) {
        Stream.of(
                ClientConfiguration.class.getName(),
                AwsClientBuilder.class.getName(),
                AwsRegionProviderChain.class.getName(),
                InstanceMetadataRegionProvider.class.getName())
                .map(RuntimeInitializedClassBuildItem::new)
                .forEach(runtimeInitialized::produce);
    }

    @BuildStep
    void reflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        // The camel component supports only this AWSS3V4Signer method, might need to register other AbstractAWSSigner subclass if it changes
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(AWSS3V4Signer.class).methods().constructors().build());

        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(Token.class).methods().constructors().build());

        // TODO: to remove when https://github.com/IBM/ibm-cos-sdk-java/issues/74 is implemented, released and integrated in Camel
        reflectiveClasses
                .produce(ReflectiveClassBuildItem.builder(ScheduledThreadPoolExecutor.class).methods().constructors().build());
    }

    @BuildStep
    NativeImageProxyDefinitionBuildItem nativeImageProxyDefinitions() {
        return new NativeImageProxyDefinitionBuildItem("org.apache.http.conn.HttpClientConnectionManager",
                "org.apache.http.pool.ConnPoolControl", "com.ibm.cloud.objectstorage.http.conn.Wrapped");
    }

    @BuildStep
    void initializeRandomRelatedClassesAtRuntime(
            BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClasses) {
        Stream.of(
                PredefinedBackoffStrategies.class.getName(),
                PredefinedRetryPolicies.class.getName(),
                "com.ibm.cloud.objectstorage.auth.BaseCredentialsFetcher",
                DefaultAWSCredentialsProviderChain.class.getName(),
                InstanceProfileCredentialsProvider.class.getName())
                .map(RuntimeInitializedClassBuildItem::new)
                .forEach(runtimeInitializedClasses::produce);
    }

    @BuildStep
    void initializeCleanerRelatedClassesAtRuntime(
            BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClasses) {
        Stream.of(
                "com.ibm.cloud.objectstorage.event.SDKProgressPublisher$LazyHolder",
                SDKProgressPublisher.class.getName())
                .map(RuntimeInitializedClassBuildItem::new)
                .forEach(runtimeInitializedClasses::produce);
    }

}
