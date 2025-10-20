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
package org.apache.camel.quarkus.component.jt400.deployment;

import java.util.ArrayList;
import java.util.List;
import java.util.ListResourceBundle;
import java.util.regex.Pattern;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.ConvTable;
import com.ibm.as400.access.NLSImplNative;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.*;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.gizmo.Gizmo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class Jt400Processor {

    private static final Logger LOG = Logger.getLogger(Jt400Processor.class);
    private static final String FEATURE = "camel-jt400";
    private static final DotName CONV_TABLE_NAME = DotName.createSimple(ConvTable.class.getName());
    private static final DotName LIST_RESOURCE_BUNDLE_NAME = DotName.createSimple(ListResourceBundle.class.getName());

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    List<RuntimeInitializedClassBuildItem> runtimeInitializedClasses() {
        List<RuntimeInitializedClassBuildItem> items = new ArrayList<>();
        items.add(new RuntimeInitializedClassBuildItem("com.ibm.as400.access.CredentialVault"));
        return items;
    }

    @BuildStep
    NativeImageEnableAllCharsetsBuildItem charset() {
        return new NativeImageEnableAllCharsetsBuildItem();
    }

    @BuildStep
    RuntimeInitializedClassBuildItem runtimeInitializedClass() {
        return new RuntimeInitializedClassBuildItem(AS400.class.getName());
    }

    @BuildStep
    void reflectiveClasses(BuildProducer<ReflectiveClassBuildItem> reflectiveClassesProducer,
            CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getIndex();

        reflectiveClassesProducer.produce(ReflectiveClassBuildItem.builder(NLSImplNative.class).build());
        reflectiveClassesProducer.produce(ReflectiveClassBuildItem.builder("com.ibm.as400.access.SocketContainerInet").build());

        Pattern pattern = Pattern.compile("com.ibm.as400.access.*Remote");
        index.getKnownClasses().stream()
                .filter(c -> pattern.matcher(c.name().toString()).matches())
                .map(c -> ReflectiveClassBuildItem.builder(c.name().toString()).build())
                .forEach(reflectiveClassesProducer::produce);

        combinedIndex.getIndex()
                .getAllKnownSubclasses(CONV_TABLE_NAME)
                .stream()
                .map(c -> ReflectiveClassBuildItem.builder(c.name().toString()).build())
                .forEach(reflectiveClassesProducer::produce);

    }

    @BuildStep
    void resourceBundles(BuildProducer<NativeImageResourceBundleBuildItem> imageResourceBundles,
            CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getIndex();

        index.getAllKnownSubclasses(LIST_RESOURCE_BUNDLE_NAME).stream()
                .filter(cl -> cl.name().toString().startsWith("com.ibm.as400"))
                .map(c -> new NativeImageResourceBundleBuildItem(c.name().toString()))
                .forEach(imageResourceBundles::produce);
    }

    @BuildStep
    IndexDependencyBuildItem registerDependencyForIndex() {
        return new IndexDependencyBuildItem("net.sf.jt400", "jt400", "java11");
    }

    @BuildStep
    BytecodeTransformerBuildItem patchToolboxSignonHandler() {
        return new BytecodeTransformerBuildItem.Builder()
                .setClassToTransform("com.ibm.as400.access.ToolboxSignonHandler")
                .setCacheable(true)
                .setVisitorFunction((className, classVisitor) -> new ToolboxSignonHandlerClassVisitor(classVisitor)).build();
    }

    /**
     * ToolboxSignonHandler starts GUI dialogues from several methods. This is not supported in the native.
     * Unfortunately the method AS400.isGuiAvailable does not disable every call to dialogue.
     * Two methods `handleSignon` and `handlePasswordChange` has to be removed (they does not contain any logic,
     * just the GUI interaction)
     *
     * ToolboxSignonHandler is a final class, therefore no substitutions can be added to this class
     * and bytecode manipulation had to be used instead.
     */
    static class ToolboxSignonHandlerClassVisitor extends ClassVisitor {

        private final Logger logger;

        protected ToolboxSignonHandlerClassVisitor(ClassVisitor classVisitor) {
            super(Gizmo.ASM_API_VERSION, classVisitor);

            logger = Logger.getLogger("ToolboxSignonHandlerClassVisitor");
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                String[] exceptions) {
            MethodVisitor original = super.visitMethod(access, name, descriptor, signature, exceptions);
            if ("handleSignon".equals(name) || "handlePasswordChange".equals(name)) {
                logger.debug("GUI interaction enhancer for Quarkus: transforming " + name + " to avoid spawning dialogues");
                // Replace the method body
                return new MethodVisitor(Gizmo.ASM_API_VERSION, original) {
                    @Override
                    public void visitCode() {
                        super.visitCode();
                        // Load 'true' or `false` onto the stack and return
                        if ("handlePasswordChange".equals(name)) {
                            visitInsn(Opcodes.ICONST_0); // push false
                        } else {
                            visitInsn(Opcodes.ICONST_1); // push true
                        }
                        visitInsn(Opcodes.IRETURN); // return boolean from the method
                    }

                    @Override
                    public void visitMaxs(int maxStack, int maxLocals) {
                        // Max stack is 1 (for the boolean), locals can remain unchanged
                        super.visitMaxs(1, 0);
                    }
                };
            }
            return original;
        }
    }

}
