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
package org.apache.camel.quarkus.component.milvus.deployment;

import java.util.List;
import java.util.Set;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

class MilvusProcessor {

    /**
     * Intercepts all Milvus SDK classes during the Quarkus build process to perform
     * bytecode transformation. This ensures the SDK points to the correct
     * gRPC/Netty implementations, avoiding 'Class Not Found'
     */

    @BuildStep
    void relocateAllShadedCalls(
            CombinedIndexBuildItem index,
            BuildProducer<BytecodeTransformerBuildItem> transformers) {

        index.getIndex().getKnownClasses().stream()
                .filter(ci -> {
                    String name = ci.name().toString();
                    return name.startsWith("io.milvus");
                })
                .forEach(ci -> {
                    transformers.produce(new BytecodeTransformerBuildItem(
                            ci.name().toString(),
                            (name, cv) -> new ShadedRelocationVisitor(cv)));
                });
    }

    @BuildStep
    List<IndexDependencyBuildItem> indexDependencies() {
        return List.of(
                new IndexDependencyBuildItem("io.milvus", "milvus-sdk-java"),
                new IndexDependencyBuildItem("com.google.protobuf", "protobuf-java"));
    }

    /**
     * Registers gRPC Service Providers (SPI) for the Native executable.
     * In Native mode, GraalVM does not automatically discover services in
     * META-INF/services.
     */

    @BuildStep
    void registerGrpcServiceProvider(BuildProducer<ServiceProviderBuildItem> services) {

        services.produce(new ServiceProviderBuildItem(
                "io.grpc.ManagedChannelProvider",
                "io.grpc.netty.NettyChannelProvider"));
        services.produce(new ServiceProviderBuildItem(
                "io.grpc.NameResolverProvider",
                "io.grpc.internal.DnsNameResolverProvider"));

        services.produce(new ServiceProviderBuildItem("io.grpc.LoadBalancerProvider",
                "io.grpc.internal.PickFirstLoadBalancerProvider",
                "io.grpc.util.OutlierDetectionLoadBalancerProvider",
                "io.grpc.util.RandomSubsettingLoadBalancerProvider",
                "io.grpc.util.SecretRoundRobinLoadBalancerProvider"));
    }

    @BuildStep
    void registerReflection(CombinedIndexBuildItem index, BuildProducer<ReflectiveClassBuildItem> reflection) {

        // Infrastructure & Clients: gRPC, Netty, and Milvus
        reflection.produce(ReflectiveClassBuildItem.builder(
                "io.grpc.internal.DnsNameResolverProvider",
                "io.grpc.internal.PickFirstLoadBalancerProvider",
                "io.grpc.netty.NettyChannelProvider",
                "io.grpc.util.SecretRoundRobinLoadBalancerProvider$Provider",
                "io.grpc.netty.GrpcSslContexts",
                "io.grpc.netty.UdsNettyChannelProvider",
                "io.grpc.netty.UdsNameResolverProvider",
                "io.milvus.client.MilvusServiceClient")
                .methods().fields().build());

        // Data Models: Recursive scan for Milvus/Protobuf
        Set<String> targetPackages = Set.of(
                "io.milvus.grpc",
                "com.google.protobuf.DescriptorProtos");

        index.getIndex().getKnownClasses().stream()
                .map(ci -> ci.name().toString())
                .filter(name -> targetPackages.stream().anyMatch(name::startsWith))
                .forEach(name -> reflection.produce(
                        ReflectiveClassBuildItem.builder(name).methods().fields().build()));
    }

    @BuildStep
    List<RuntimeInitializedClassBuildItem> runtimeInit() {
        return List.of(
                // Forces the gRPC Utils and its inner holders to initialize at runtime
                new RuntimeInitializedClassBuildItem("io.grpc.netty.Utils"),
                new RuntimeInitializedClassBuildItem("io.grpc.netty.Utils$ByteBufAllocatorPreferDirectHolder"),
                new RuntimeInitializedClassBuildItem("io.grpc.netty.Utils$ByteBufAllocatorPreferHeapHolder"),
                new RuntimeInitializedClassBuildItem("io.grpc.internal.RetriableStream"));

    }

    /**
     * Custom ClassRemapper used during the Quarkus build step to intercept and
     * redirect shaded Netty/gRPC calls within the Milvus SDK.
     */

    private static class ShadedRelocationVisitor extends ClassRemapper {
        public ShadedRelocationVisitor(ClassVisitor cv) {
            super(Opcodes.ASM9, cv, new Remapper(Opcodes.ASM9) {
                @Override
                public String map(String internalName) {
                    if (internalName == null)
                        return null;

                    if (internalName.startsWith("io/grpc/netty/shaded/io/grpc")) {
                        return internalName.replace("io/grpc/netty/shaded/io/grpc", "io/grpc");
                    }
                    if (internalName.startsWith("io/grpc/netty/shaded/io/netty")) {
                        return internalName.replace("io/grpc/netty/shaded/io/netty", "io/netty");
                    }
                    return super.map(internalName);
                }
            });
        }
    }

}
