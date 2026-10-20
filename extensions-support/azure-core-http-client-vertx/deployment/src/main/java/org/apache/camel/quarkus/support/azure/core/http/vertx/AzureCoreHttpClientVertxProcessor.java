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
package org.apache.camel.quarkus.support.azure.core.http.vertx;

import com.azure.core.http.vertx.VertxProvider;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.gizmo.Gizmo;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;

public class AzureCoreHttpClientVertxProcessor {

    @BuildStep
    void runtimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClasses) {
        runtimeInitializedClasses.produce(new RuntimeInitializedClassBuildItem(
                "com.azure.core.http.vertx.VertxHttpClientProvider$GlobalVertxHttpClient"));
        runtimeInitializedClasses.produce(
                new RuntimeInitializedClassBuildItem("com.azure.core.http.vertx.VertxHttpClientBuilder$DefaultVertx"));
    }

    @BuildStep
    void registerServiceProviders(BuildProducer<ServiceProviderBuildItem> serviceProvider) {
        serviceProvider.produce(ServiceProviderBuildItem.allProvidersFromClassPath(VertxProvider.class.getName()));
    }

    @BuildStep
    BytecodeTransformerBuildItem transformVertxHttpClient() {
        // azure-core-http-vertx is compiled against Vert.x 4, where HttpClientRequest.reset(long, Throwable) returns
        // boolean. In Vert.x 5 it returns Future<Void>, so the vertxRequest::reset method reference fails to link and
        // requests with a streamed body hang. The reference targets a BiConsumer, so the return value is discarded
        // either way. TODO: Remove when https://github.com/apache/camel-quarkus/issues/9360 is resolved
        return new BytecodeTransformerBuildItem.Builder()
                .setClassToTransform("com.azure.core.http.vertx.VertxHttpClient")
                .setCacheable(true)
                .setVisitorFunction((className, classVisitor) -> new ClassVisitor(Gizmo.ASM_API_VERSION, classVisitor) {
                    @Override
                    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                            String[] exceptions) {
                        MethodVisitor visitor = super.visitMethod(access, name, descriptor, signature, exceptions);
                        return new MethodVisitor(Gizmo.ASM_API_VERSION, visitor) {
                            @Override
                            public void visitInvokeDynamicInsn(String methodName, String methodDescriptor,
                                    Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
                                for (int i = 0; i < bootstrapMethodArguments.length; i++) {
                                    if (bootstrapMethodArguments[i] instanceof Handle handle
                                            && handle.getOwner().equals("io/vertx/core/http/HttpClientRequest")
                                            && handle.getName().equals("reset")
                                            && handle.getDesc().equals("(JLjava/lang/Throwable;)Z")) {
                                        bootstrapMethodArguments[i] = new Handle(handle.getTag(), handle.getOwner(),
                                                handle.getName(), "(JLjava/lang/Throwable;)Lio/vertx/core/Future;",
                                                handle.isInterface());
                                    }
                                }
                                super.visitInvokeDynamicInsn(methodName, methodDescriptor, bootstrapMethodHandle,
                                        bootstrapMethodArguments);
                            }
                        };
                    }
                })
                .build();
    }
}
