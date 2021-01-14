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
package org.apache.camel.quarkus.component.azure.storage.blob.deployment;

import java.util.stream.Stream;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

class AzureStorageBlobProcessor {

    private static final String FEATURE = "camel-azure-storage-blob";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(FEATURE);
    }

    @BuildStep
    void runtimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClasses) {
        Stream.of(
                "io.netty.handler.ssl.OpenSsl",
                "io.netty.internal.tcnative.SSL",
                "io.netty.util.NetUtil",
                "reactor.netty.http.client.HttpClient",
                "reactor.netty.tcp.TcpClient",
                "reactor.netty.resources.DefaultLoopNativeDetector",
                "reactor.netty.resources.DefaultLoopEpoll",
                "reactor.netty.resources.DefaultLoopKQueue",
                "reactor.netty.resources.MicrometerPooledConnectionProviderMeterRegistrar",
                "reactor.netty.Metrics")
                .map(RuntimeInitializedClassBuildItem::new)
                .forEach(runtimeInitializedClasses::produce);
    }

    @BuildStep
    void proxyDefinitions(BuildProducer<NativeImageProxyDefinitionBuildItem> proxyDefinitions) {
        Stream.of(
                "com.azure.storage.blob.implementation.AppendBlobsImpl$AppendBlobsService",
                "com.azure.storage.blob.implementation.BlobsImpl$BlobsService",
                "com.azure.storage.blob.implementation.BlockBlobsImpl$BlockBlobsService",
                "com.azure.storage.blob.implementation.ContainersImpl$ContainersService",
                "com.azure.storage.blob.implementation.DirectorysImpl$DirectorysService",
                "com.azure.storage.blob.implementation.PageBlobsImpl$PageBlobsService",
                "com.azure.storage.blob.implementation.ServicesImpl$ServicesService")
                .map(NativeImageProxyDefinitionBuildItem::new)
                .forEach(proxyDefinitions::produce);
    }

    @BuildStep
    void reflectiveClasses(CombinedIndexBuildItem combinedIndex, BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {

        final String[] modelClasses = combinedIndex
                .getIndex()
                .getKnownClasses()
                .stream()
                .map(ClassInfo::name)
                .map(DotName::toString)
                .filter(n -> n.startsWith("com.azure.storage.blob.implementation.models.")
                        || n.startsWith("com.azure.storage.blob.models."))
                .sorted()
                .toArray(String[]::new);
        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, true, modelClasses));

        reflectiveClasses.produce(new ReflectiveClassBuildItem(false, false, "com.azure.core.util.DateTimeRfc1123"));

    }

    @BuildStep
    IndexDependencyBuildItem indexDependency() {
        return new IndexDependencyBuildItem("com.azure", "azure-storage-blob");
    }

}
