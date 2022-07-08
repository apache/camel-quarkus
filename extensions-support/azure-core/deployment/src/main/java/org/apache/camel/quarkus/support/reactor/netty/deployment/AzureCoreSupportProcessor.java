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
import java.util.Set;
import java.util.stream.Stream;

import com.azure.core.annotation.ServiceInterface;
import com.azure.core.http.HttpClientProvider;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.util.ServiceUtil;
import org.jboss.jandex.DotName;

public class AzureCoreSupportProcessor {
    private static final DotName SERVICE_INTERFACE_DOT_NAME = DotName.createSimple(ServiceInterface.class.getName());

    @BuildStep
    IndexDependencyBuildItem indexDependency() {
        return new IndexDependencyBuildItem("com.azure", "azure-core");
    }

    @BuildStep
    void reflectiveClasses(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        reflectiveClasses.produce(new ReflectiveClassBuildItem(false, false,
                com.azure.core.util.DateTimeRfc1123.class,
                com.azure.core.http.rest.StreamResponse.class,
                com.azure.core.http.rest.ResponseBase.class));

        reflectiveClasses.produce(new ReflectiveClassBuildItem(false, true,
                "com.microsoft.aad.msal4j.AadInstanceDiscoveryResponse",
                "com.microsoft.aad.msal4j.InstanceDiscoveryMetadataEntry"));

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

}
