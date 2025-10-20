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
package org.apache.camel.quarkus.support.reactor.netty.deployment;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.azure.core.annotation.ServiceInterface;
import com.azure.core.exception.HttpResponseException;
import com.azure.core.http.HttpClientProvider;
import com.azure.json.JsonSerializable;
import com.azure.xml.XmlSerializable;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.util.ServiceUtil;
import io.smallrye.common.os.OS;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

public class AzureCoreSupportProcessor {
    private static final DotName SERVICE_INTERFACE_DOT_NAME = DotName.createSimple(ServiceInterface.class.getName());

    @BuildStep
    void indexDependency(BuildProducer<IndexDependencyBuildItem> indexedDependencies) {
        indexedDependencies.produce(new IndexDependencyBuildItem("com.azure", "azure-core"));
        indexedDependencies.produce(new IndexDependencyBuildItem("com.azure", "azure-identity"));
    }

    @BuildStep
    void reflectiveClasses(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(com.azure.core.util.DateTimeRfc1123.class,
                com.azure.core.http.HttpHeaderName.class,
                com.azure.core.http.rest.StreamResponse.class,
                com.azure.core.http.rest.ResponseBase.class).build());

        reflectiveClasses.produce(ReflectiveClassBuildItem.builder("com.microsoft.aad.msal4j.AadInstanceDiscoveryResponse",
                "com.microsoft.aad.msal4j.InstanceDiscoveryMetadataEntry").fields().build());

        // HttpResponseException instances may be dynamically instantiated and have methods invoked reflectively
        Set<String> httpResponseExceptionClasses = combinedIndex.getIndex()
                .getAllKnownSubclasses(HttpResponseException.class)
                .stream()
                .map(ClassInfo::name)
                .map(DotName::toString)
                .collect(Collectors.toUnmodifiableSet());

        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(httpResponseExceptionClasses.toArray(new String[0]))
                .methods()
                .build());

        // implementations of serializers are used during errors reporting
        LinkedHashSet<String> serializers = new LinkedHashSet<>(
                combinedIndex.getIndex().getAllKnownImplementations(JsonSerializable.class).stream()
                        .map(ci -> ci.name().toString())
                        .toList());
        serializers.addAll(combinedIndex.getIndex().getAllKnownImplementations(XmlSerializable.class).stream()
                .map(ci -> ci.name().toString())
                .toList());

        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(serializers.toArray(new String[0]))
                .methods()
                .fields()
                .build());
    }

    @BuildStep
    void nativeResources(BuildProducer<ServiceProviderBuildItem> services,
            BuildProducer<NativeImageResourceBuildItem> nativeResources) {
        Stream.of(
                HttpClientProvider.class.getName(), // TODO move this to a separate camel-quarkus-azure-core extension
                "reactor.blockhound.integration.BlockHoundIntegration" // TODO: move to reactor extension

        )
                .forEach(service -> {
                    try {
                        Set<String> implementations = ServiceUtil.classNamesNamedIn(
                                Thread.currentThread().getContextClassLoader(),
                                "META-INF/services/" + service);
                        services.produce(
                                new ServiceProviderBuildItem(service,
                                        implementations.toArray(new String[0])));

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

        nativeResources.produce(new NativeImageResourceBuildItem(
                "azure-core.properties"));
    }

    @BuildStep
    void proxyDefinitions(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<NativeImageProxyDefinitionBuildItem> proxyDefinitions) {

        combinedIndex
                .getIndex()
                .getAnnotations(SERVICE_INTERFACE_DOT_NAME)
                .stream()
                .map(annotationInstance -> annotationInstance.target().asClass().name().toString())
                .map(NativeImageProxyDefinitionBuildItem::new)
                .forEach(proxyDefinitions::produce);
    }

    @BuildStep(onlyIf = Msal4jAndIdentityIsPresent.class)
    void enableLoadingOfNativeLibraries(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClass) {
        OS os = OS.current();
        if (os.equals(OS.LINUX) || os.equals(OS.MAC)) {
            String iSecurityLibraryClassName = "com.microsoft.aad.msal4jextensions.persistence.%s.ISecurityLibrary"
                    .formatted(os.name().toLowerCase());
            runtimeInitializedClass.produce(new RuntimeInitializedClassBuildItem(iSecurityLibraryClassName));
        }

        if (os.equals(OS.WINDOWS)) {
            runtimeInitializedClass.produce(new RuntimeInitializedClassBuildItem("com.sun.jna.platform.win32.Crypt32"));
            runtimeInitializedClass.produce(new RuntimeInitializedClassBuildItem("com.sun.jna.platform.win32.Kernel32"));
        }
    }

    public static final class Msal4jAndIdentityIsPresent implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            try {
                Thread.currentThread().getContextClassLoader().loadClass("com.microsoft.aad.msal4j.Credential");
                Thread.currentThread().getContextClassLoader().loadClass("com.azure.identity.implementation.IdentityClient");
                return true;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }
    }
}
