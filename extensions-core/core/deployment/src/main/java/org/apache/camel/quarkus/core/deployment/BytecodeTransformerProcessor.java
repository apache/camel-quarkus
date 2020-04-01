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
package org.apache.camel.quarkus.core.deployment;

import java.util.function.BiFunction;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.gizmo.Gizmo;
import org.apache.camel.quarkus.support.common.CamelCapabilities;
import org.apache.camel.reifier.rest.RestBindingReifier;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.NEW;

class BytecodeTransformerProcessor {
    @BuildStep
    void transformRestBindingReifier(Capabilities capabilities,
            BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformer) {
        // if jaxb is configured, don't replace the method
        if (capabilities.isCapabilityPresent(CamelCapabilities.XML_JAXB)) {
            return;
        }

        bytecodeTransformer.produce(
                new BytecodeTransformerBuildItem(
                        RestBindingReifier.class.getName(),
                        new RestBindingReifierTransformer()));
    }

    private static class RestBindingReifierTransformer implements BiFunction<String, ClassVisitor, ClassVisitor> {
        @Override
        public ClassVisitor apply(String s, ClassVisitor classVisitor) {
            return new ClassVisitor(Gizmo.ASM_API_VERSION, classVisitor) {
                @Override
                public MethodVisitor visitMethod(
                        int access,
                        String name,
                        String descriptor,
                        String signature,
                        String[] exceptions) {

                    final MethodVisitor target = super.visitMethod(access, name, descriptor, signature, exceptions);

                    if (name.equals("setupJaxb")) {
                        return new MethodVisitor(Gizmo.ASM_API_VERSION, null) {
                            @Override
                            public void visitCode() {
                                target.visitCode();
                                target.visitTypeInsn(NEW, "java/lang/UnsupportedOperationException");
                                target.visitInsn(DUP);
                                target.visitLdcInsn("Please add a dependency to camel-quarkus-xml-jaxb");
                                target.visitMethodInsn(
                                        INVOKESPECIAL,
                                        "java/lang/UnsupportedOperationException", "<init>",
                                        "(Ljava/lang/String;)V",
                                        false);
                                target.visitInsn(ATHROW);
                                target.visitMaxs(2, 0);
                                target.visitEnd();
                            }
                        };
                    }
                    return target;
                }
            };
        }
    }
}
