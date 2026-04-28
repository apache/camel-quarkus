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

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
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
                    return name.startsWith("io.milvus") && !name.startsWith("io.milvus.shaded");
                })
                .forEach(ci -> {
                    transformers.produce(new BytecodeTransformerBuildItem(
                            ci.name().toString(),
                            (name, cv) -> new ShadedRelocationVisitor(cv)));
                });
    }

    //This build step ensures that the Milvus Java SDK is indexed by Jandex.

    @BuildStep
    IndexDependencyBuildItem indexDependencie() {
        return new IndexDependencyBuildItem("io.milvus", "milvus-sdk-java");
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

                    if (internalName.startsWith("io/milvus/shaded/io/grpc/netty/shaded/io/grpc")) {
                        return internalName.replace("io/milvus/shaded/io/grpc/netty/shaded/io/grpc", "io/grpc");
                    }
                    if (internalName.startsWith("io/milvus/shaded/io/grpc/netty/shaded/io/netty")) {
                        return internalName.replace("io/milvus/shaded/io/grpc/netty/shaded/io/netty", "io/netty");
                    }
                    if (internalName.startsWith("io/milvus/shaded/io/grpc")) {
                        return internalName.replace("io/milvus/shaded/io/grpc", "io/grpc");
                    }
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
