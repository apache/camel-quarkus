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

import com.azure.identity.implementation.IdentityClient;
import com.azure.identity.implementation.IdentityClientBase;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
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
        reflectiveClasses.produce(new ReflectiveClassBuildItem(false, true, modelClasses));

    }

    @BuildStep
    IndexDependencyBuildItem indexDependency() {
        return new IndexDependencyBuildItem("com.azure", "azure-storage-blob");
    }

    @BuildStep
    void nativeResources(BuildProducer<NativeImageResourceBuildItem> nativeResources) {
        nativeResources.produce(new NativeImageResourceBuildItem(
                "azure-storage-blob.properties"));
    }

    @BuildStep
    void runtimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClass) {
        // Required by azure-identity
        runtimeInitializedClass.produce(new RuntimeInitializedClassBuildItem("com.sun.jna.platform.win32.Crypt32"));
        runtimeInitializedClass.produce(new RuntimeInitializedClassBuildItem("com.sun.jna.platform.win32.Kernel32"));
        runtimeInitializedClass.produce(new RuntimeInitializedClassBuildItem(IdentityClient.class.getName()));
        runtimeInitializedClass.produce(
                new RuntimeInitializedClassBuildItem("com.microsoft.aad.msal4jextensions.persistence.linux.ISecurityLibrary"));
        runtimeInitializedClass.produce(
                new RuntimeInitializedClassBuildItem("com.microsoft.aad.msal4jextensions.persistence.mac.ISecurityLibrary"));
        // TODO: Remove this - https://github.com/apache/camel-quarkus/issues/4356
        runtimeInitializedClass.produce(new RuntimeInitializedClassBuildItem(IdentityClientBase.class.getName()));
    }
}
