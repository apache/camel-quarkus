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

import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExcludeDependencyBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeReinitializedClassBuildItem;
import io.quarkus.security.deployment.BouncyCastleProviderBuildItem;
import io.quarkus.security.deployment.SecurityConfig;
import org.apache.camel.quarkus.support.bouncycastle.BouncyCastleRecorder;
import org.jboss.jandex.IndexView;

public class BouncyCastleSupportProcessor {

    SecurityConfig securityConfig;

    @BuildStep(onlyIfNot = BcProviderConfigured.class) //register BC only if no BC* provider is registered
    void produceBouncyCastleProvider(BuildProducer<BouncyCastleProviderBuildItem> bouncyCastleProvider) {
        //register BC if there is no BC or BCFIPS provider in securityConfiguration
        bouncyCastleProvider.produce(new BouncyCastleProviderBuildItem());
    }

    @BuildStep()
    @Record(ExecutionTime.STATIC_INIT)
    public void registerBouncyCastleProvider(List<CipherTransformationBuildItem> cipherTransformations,
            BouncyCastleRecorder recorder,
            ShutdownContextBuildItem shutdownContextBuildItem) {
        List<String> allCipherTransformations = cipherTransformations.stream()
                .flatMap(c -> c.getCipherTransformations().stream()).collect(Collectors.toList());
        recorder.registerBouncyCastleProvider(allCipherTransformations, shutdownContextBuildItem);
    }

    @BuildStep()
    ReflectiveClassBuildItem registerForReflection(CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getIndex();

        String[] dtos = index.getKnownClasses().stream()
                .map(ci -> ci.name().toString())
                .filter(n -> n.startsWith("org.bouncycastle.jcajce.provider.digest.") ||
                        n.startsWith("org.bouncycastle.jcajce.provider.symmetric.") ||
                        n.startsWith("org.bouncycastle.jcajce.provider.asymmetric.") ||
                        n.startsWith("org.bouncycastle.jcajce.provider.keystore."))
                .toArray(String[]::new);

        return ReflectiveClassBuildItem.builder(dtos).build();
    }

    @BuildStep(onlyIfNot = FipsProviderConfigured.class)
    void secureRandomConfiguration(BuildProducer<RuntimeReinitializedClassBuildItem> reinitialized) {
        reinitialized.produce(new RuntimeReinitializedClassBuildItem("java.security.SecureRandom"));
    }

    @BuildStep(onlyIf = FipsProviderConfigured.class)
    void excludeBc(BuildProducer<ExcludeDependencyBuildItem> excludeDependencies) {
        //exclude BC in FIPS environment
        excludeDependencies.produce(new ExcludeDependencyBuildItem("org.bouncycastle", "bcpkix-jdk18on"));
        excludeDependencies.produce(new ExcludeDependencyBuildItem("org.bouncycastle", "bcbcprov-jdk18on"));
        excludeDependencies.produce(new ExcludeDependencyBuildItem("org.bouncycastle", "bcutil-jdk18on"));
    }

    /**
     * Indicates whether FIPS provider is registered via quarkus.security.
     */
    static final class FipsProviderConfigured implements BooleanSupplier {
        SecurityConfig securityConfig;

        @Override
        public boolean getAsBoolean() {
            return securityConfig.securityProviders().orElse(Collections.emptySet()).stream()
                    .filter(p -> p.toLowerCase().contains("fips")).findAny().isPresent();

        }
    }

    /**
     * Indicates whether BC* provider is registered via quarkus.security.
     */
    static final class BcProviderConfigured implements BooleanSupplier {
        SecurityConfig securityConfig;

        @Override
        public boolean getAsBoolean() {
            return securityConfig.securityProviders().orElse(Collections.emptySet()).stream()
                    .filter(p -> p.toLowerCase().startsWith("bc")).findAny().isPresent();

        }
    }

}
