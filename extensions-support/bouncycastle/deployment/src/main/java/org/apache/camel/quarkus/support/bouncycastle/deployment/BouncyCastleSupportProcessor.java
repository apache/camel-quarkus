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
package org.apache.camel.quarkus.support.bouncycastle.deployment;

import java.util.List;
import java.util.stream.Collectors;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeReinitializedClassBuildItem;
import io.quarkus.security.deployment.BouncyCastleProviderBuildItem;
import org.apache.camel.quarkus.support.bouncycastle.BouncyCastleRecorder;
import org.jboss.jandex.IndexView;

public class BouncyCastleSupportProcessor {

    @BuildStep
    void produceBouncyCastleProvider(BuildProducer<BouncyCastleProviderBuildItem> bouncyCastleProvider) {
        bouncyCastleProvider.produce(new BouncyCastleProviderBuildItem());
    }

    @BuildStep
    ReflectiveClassBuildItem registerForReflection(CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getIndex();

        String[] dtos = index.getKnownClasses().stream()
                .map(ci -> ci.name().toString())
                .filter(n -> n.startsWith("org.bouncycastle.jcajce.provider.digest.") ||
                        n.startsWith("org.bouncycastle.jcajce.provider.symmetric.") ||
                        n.startsWith("org.bouncycastle.jcajce.provider.asymmetric.") ||
                        n.startsWith("org.bouncycastle.jcajce.provider.keystore."))
                .toArray(String[]::new);

        return new ReflectiveClassBuildItem(false, false, dtos);
    }

    @BuildStep
    IndexDependencyBuildItem registerBCDependencyForIndex() {
        return new IndexDependencyBuildItem("org.bouncycastle", "bcprov-jdk18on");
    }

    @BuildStep
    void secureRandomConfiguration(BuildProducer<RuntimeReinitializedClassBuildItem> reinitialized) {
        reinitialized.produce(new RuntimeReinitializedClassBuildItem("java.security.SecureRandom"));
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public void registerBouncyCastleProvider(List<CipherTransformationBuildItem> cipherTransformations,
            BouncyCastleRecorder recorder,
            ShutdownContextBuildItem shutdownContextBuildItem) {
        List<String> allCipherTransformations = cipherTransformations.stream()
                .flatMap(c -> c.getCipherTransformations().stream()).collect(Collectors.toList());
        recorder.registerBouncyCastleProvider(allCipherTransformations, shutdownContextBuildItem);
    }
}
