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
package org.apache.camel.quarkus.component.jackson.avro.deployment;

import java.util.function.BooleanSupplier;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.gizmo.Gizmo;
import org.apache.avro.Schema.Parser;
import org.apache.avro.file.DataFileWriter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class JacksonAvroProcessor {
    private static final String FEATURE = "camel-jackson-avro";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    RuntimeInitializedClassBuildItem runtimeInitializedClass() {
        return new RuntimeInitializedClassBuildItem(DataFileWriter.class.getName());
    }

    @BuildStep(onlyIfNot = { AvroParserSetValidateMethodPresent.class })
    BytecodeTransformerBuildItem patchAvroJacksonCompatibility() {
        // Hack to maintain avro-jackson compatibility with Avro 1.12.x
        // Adds a noop version of Parser.setValidate that got removed in Avro 1.12.x.
        // public Parser setValidate(boolean validate) {
        //     return this;
        // }
        return new BytecodeTransformerBuildItem.Builder()
                .setClassToTransform(Parser.class.getName())
                .setCacheable(true)
                .setVisitorFunction((className, classVisitor) -> {
                    return new ClassVisitor(Gizmo.ASM_API_VERSION, classVisitor) {
                        @Override
                        public void visitEnd() {
                            MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, "setValidate",
                                    "(Z)Lorg/apache/avro/Schema$Parser;", null, null);
                            if (mv != null) {
                                mv.visitCode();
                                mv.visitVarInsn(Opcodes.ALOAD, 0);
                                mv.visitInsn(Opcodes.ARETURN);
                                mv.visitMaxs(1, 2);
                                mv.visitEnd();
                            }
                            super.visitEnd();
                        }
                    };
                })
                .build();
    }

    static class AvroParserSetValidateMethodPresent implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            try {
                Parser.class.getDeclaredMethod("setValidate", boolean.class);
                return true;
            } catch (NoSuchMethodException e) {
                return false;
            }
        }
    }
}
