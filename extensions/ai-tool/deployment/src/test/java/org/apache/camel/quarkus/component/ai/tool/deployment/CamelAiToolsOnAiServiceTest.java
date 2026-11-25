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
package org.apache.camel.quarkus.component.ai.tool.deployment;

import java.util.List;
import java.util.logging.Level;

import io.quarkus.test.QuarkusExtensionTest;
import org.apache.camel.quarkus.component.ai.tool.CamelAiTools;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@code @CamelAiTools} on a {@code @RegisterAiService} interface must not warn; on a plain interface it must.
 * The AI service interface is generated with ASM because {@code @RegisterAiService} is only added at test runtime
 * (forced dependency), not at compile time.
 */
class CamelAiToolsOnAiServiceTest {

    private static final String AI_SERVICE = CamelAiToolsOnAiServiceTest.class.getPackageName() + ".GeneratedAiService";

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(PlainService.class, OptionalDependencies.class)
                    .add(new ByteArrayAsset(aiServiceInterface()), AI_SERVICE.replace('.', '/') + ".class"))
            .setForcedDependencies(
                    List.of(OptionalDependencies.QUARKUS_LANGCHAIN4J, OptionalDependencies.CAMEL_LANGCHAIN4J_AGENT))
            .setLogRecordPredicate(r -> r.getLevel().equals(Level.WARNING)
                    && r.getMessage().contains("only applies to @RegisterAiService interfaces"))
            .assertLogRecords(records -> assertEquals(List.of(PlainService.class.getName()),
                    records.stream().map(r -> String.valueOf(r.getParameters()[0])).toList()));

    @Test
    void warnsOnlyForPlainInterface() {
        // Asserted by assertLogRecords above
    }

    /** {@code @RegisterAiService @CamelAiTools("x") public interface GeneratedAiService {}} */
    private static byte[] aiServiceInterface() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT,
                AI_SERVICE.replace('.', '/'), null, "java/lang/Object", null);
        cw.visitAnnotation("Lio/quarkiverse/langchain4j/RegisterAiService;", true).visitEnd();
        AnnotationVisitor tools = cw.visitAnnotation(Type.getDescriptor(CamelAiTools.class), true);
        tools.visit("value", "x");
        tools.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    @CamelAiTools("x")
    public interface PlainService {
    }
}
