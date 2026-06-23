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
package org.apache.camel.quarkus.component.micrometer.deployment;

import java.util.List;
import java.util.Optional;

import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.builder.Version;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.micrometer.deployment.MicrometerProcessor.MicrometerEnabled;
import io.quarkus.micrometer.deployment.MicrometerRegistryProviderBuildItem;
import io.quarkus.micrometer.deployment.RootMeterRegistryBuildItem;
import org.apache.camel.component.micrometer.MicrometerConstants;
import org.apache.camel.quarkus.component.micrometer.CamelMicrometerConfig;
import org.apache.camel.quarkus.component.micrometer.CamelMicrometerRecorder;
import org.apache.camel.quarkus.core.deployment.spi.CamelBeanBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelContextCustomizerBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.RuntimeCamelContextCustomizerBuildItem;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class MicrometerProcessor {

    private static final String FEATURE = "camel-micrometer";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep(onlyIf = MicrometerEnabled.class)
    CamelBeanBuildItem meterRegistry(RootMeterRegistryBuildItem registry) {
        return new CamelBeanBuildItem(
                MicrometerConstants.METRICS_REGISTRY_NAME,
                MeterRegistry.class.getName(),
                registry.getValue());
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep(onlyIf = MicrometerEnabled.class)
    CamelContextCustomizerBuildItem contextCustomizer(
            CamelMicrometerRecorder recorder,
            CamelMicrometerConfig config) {

        return new CamelContextCustomizerBuildItem(recorder.createContextCustomizer(config));
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep(onlyIf = MicrometerEnabled.class)
    RuntimeCamelContextCustomizerBuildItem runtimeContextCustomizer(
            RootMeterRegistryBuildItem rootMeterRegistryBuildItem,
            CamelMicrometerRecorder recorder,
            CamelMicrometerConfig config) {
        return new RuntimeCamelContextCustomizerBuildItem(
                recorder.createRuntimeContextCustomizer(config, rootMeterRegistryBuildItem.getValue()));
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void configureDefaultRegistry(
            List<MicrometerRegistryProviderBuildItem> providers,
            Optional<RootMeterRegistryBuildItem> registry,
            CamelMicrometerRecorder recorder) {
        // Register SimpleMeterRegistry to the CompositeMeterRegistry (created by Quarkus) if there is no MicrometerRegistryProviderBuildItem
        if (registry.isPresent() && providers.isEmpty()) {
            recorder.configureDefaultRegistry(registry.get().getValue());
        }
    }

    @BuildStep
    BytecodeTransformerBuildItem generateRuntimeInfoMetricVersion() {
        // Force the app.info metric version tag value, to avoid runtime usages of reflection
        // and other dynamic version lookup strategies that may not work with all Quarkus package
        // types. Such as uber-jar and native images
        String quarkusVersion = Version.getVersion();
        return new BytecodeTransformerBuildItem.Builder()
                .setClassToTransform(
                        "org.apache.camel.component.micrometer.json.AbstractMicrometerService$RuntimeInfo")
                .setCacheable(true)
                .setVisitorFunction(
                        (className, classVisitor) -> new RuntimeInfoClassVisitor(classVisitor, quarkusVersion))
                .build();
    }

    static class RuntimeInfoClassVisitor extends ClassVisitor {
        private final String quarkusVersion;

        protected RuntimeInfoClassVisitor(ClassVisitor classVisitor, String quarkusVersion) {
            super(Gizmo.ASM_API_VERSION, classVisitor);
            this.quarkusVersion = quarkusVersion;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                String[] exceptions) {
            MethodVisitor original = super.visitMethod(access, name, descriptor, signature, exceptions);
            if ("scan".equals(name)) {
                return new MethodVisitor(Gizmo.ASM_API_VERSION, original) {
                    @Override
                    public void visitCode() {
                        super.visitCode();
                        visitLdcInsn(quarkusVersion);
                        visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Optional", "of",
                                "(Ljava/lang/Object;)Ljava/util/Optional;", false);
                        visitInsn(Opcodes.ARETURN);
                    }

                    @Override
                    public void visitMaxs(int maxStack, int maxLocals) {
                        super.visitMaxs(1, 1);
                    }
                };
            }
            return original;
        }
    }
}
