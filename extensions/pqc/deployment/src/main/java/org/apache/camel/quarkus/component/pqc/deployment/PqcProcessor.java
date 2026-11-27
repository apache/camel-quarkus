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
package org.apache.camel.quarkus.component.pqc.deployment;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageSecurityProviderBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.apache.camel.quarkus.support.bouncycastle.BouncyCastleRecorder;
import org.apache.camel.quarkus.support.bouncycastle.deployment.BouncyCastleAdditionalProviderBuildItem;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

class PqcProcessor {

    private static final Logger LOG = Logger.getLogger(PqcProcessor.class);
    private static final String FEATURE = "camel-pqc";
    private static final String BC_JCAJCE_PACKAGE = "org/bouncycastle/jcajce/";
    private static final String LEGACY_BC_PQC_JCAJCE_PACKAGE = "org/bouncycastle/pqc/jcajce/";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    NativeImageSecurityProviderBuildItem registerBcpqcSecurityProvider() {
        return new NativeImageSecurityProviderBuildItem("org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider");
    }

    @BuildStep
    ReflectiveClassBuildItem registerBouncyCastlePQCClasses(CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getIndex();

        String[] pqcClasses = index.getKnownClasses().stream()
                .map(ci -> ci.name().toString())
                .filter(
                        n -> (n.startsWith("org.bouncycastle.pqc.jcajce.provider")
                                && (n.endsWith("Spi") || n.contains("Spi$"))))
                .toArray(String[]::new);

        return ReflectiveClassBuildItem.builder(pqcClasses).methods().fields().build();
    }

    @BuildStep
    void registerCryptoClasses(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        reflectiveClasses.produce(
                ReflectiveClassBuildItem.builder(
                        java.security.KeyPairGenerator.class,
                        java.security.Signature.class,
                        java.security.KeyFactory.class,
                        javax.crypto.KeyGenerator.class,
                        javax.crypto.SecretKey.class).methods().build());
    }

    @BuildStep
    BouncyCastleAdditionalProviderBuildItem registerBouncyCastlePQCProvider() {
        return new BouncyCastleAdditionalProviderBuildItem(BouncyCastleRecorder.BOUNCYCASTLE_PCQ_PROVIDER_NAME);
    }

    /**
     * Workaround for camel-pqc being compiled against BouncyCastle 1.87 while the Quarkus platform manages 1.86.
     * <p>
     * BouncyCastle 1.87 moved the LMS / XMSS JCA spec and key interfaces from {@code org.bouncycastle.pqc.jcajce.*} to
     * {@code org.bouncycastle.jcajce.*}. The members camel-pqc uses are identical in both locations, so when the new
     * classes are absent, camel-pqc references are remapped to the 1.86 equivalents.
     * <p>
     * TODO: Remove when Quarkus aligns to BouncyCastle 1.87 or newer.
     */
    @BuildStep
    void remapBouncyCastleLmsXmssClasses(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<BytecodeTransformerBuildItem> transformers) {

        if (QuarkusClassLoader.isClassPresentAtRuntime("org.bouncycastle.jcajce.spec.LMSKeyGenParameterSpec")) {
            return;
        }

        LOG.debugf("BouncyCastle < 1.87 detected. Remapping camel-pqc LMS / XMSS class references to %s",
                LEGACY_BC_PQC_JCAJCE_PACKAGE.replace('/', '.'));

        combinedIndex.getIndex().getKnownClasses().stream()
                .map(ci -> ci.name().toString())
                .filter(name -> name.startsWith("org.apache.camel.component.pqc."))
                .forEach(name -> transformers.produce(new BytecodeTransformerBuildItem.Builder()
                        .setClassToTransform(name)
                        .setCacheable(true)
                        // A plain class rename does not need frames recomputing. Avoiding COMPUTE_FRAMES prevents
                        // ASM from loading optional types that may be absent (E.g. the AWS SDK used by key lifecycle managers)
                        .setInputTransformer((className, bytes) -> {
                            ClassReader reader = new ClassReader(bytes);
                            ClassWriter writer = new ClassWriter(reader, 0);
                            reader.accept(new LegacyBouncyCastleLmsXmssRemapper(writer), 0);
                            return writer.toByteArray();
                        })
                        .build()));
    }

    private static final class LegacyBouncyCastleLmsXmssRemapper extends ClassRemapper {
        LegacyBouncyCastleLmsXmssRemapper(ClassVisitor cv) {
            super(Opcodes.ASM9, cv, new Remapper(Opcodes.ASM9) {
                @Override
                public String map(String internalName) {
                    if (internalName != null && internalName.startsWith(BC_JCAJCE_PACKAGE)) {
                        String suffix = internalName.substring(BC_JCAJCE_PACKAGE.length());
                        if (isMovedLmsXmssClass(suffix, "spec/") || isMovedLmsXmssClass(suffix, "interfaces/")) {
                            return LEGACY_BC_PQC_JCAJCE_PACKAGE + suffix;
                        }
                    }
                    return super.map(internalName);
                }
            });
        }

        private static boolean isMovedLmsXmssClass(String suffix, String subPackage) {
            if (!suffix.startsWith(subPackage)) {
                return false;
            }
            String simpleName = suffix.substring(subPackage.length());
            return simpleName.indexOf('/') == -1 && (simpleName.startsWith("LMS") || simpleName.startsWith("XMSS"));
        }
    }
}
