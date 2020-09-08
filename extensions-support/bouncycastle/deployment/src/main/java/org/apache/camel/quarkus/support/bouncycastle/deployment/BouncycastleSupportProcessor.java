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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.jboss.jandex.IndexView;

public class BouncycastleSupportProcessor {
    static final String FEATURE = "camel-support-bouncycastle";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ReflectiveClassBuildItem registerForReflection(CombinedIndexBuildItem combinedIndex,
            CamelBouncycastleConfig bouncycastleConfig) {
        IndexView index = combinedIndex.getIndex();

        //gather all ciphers and digests from configuration and filter classes from index
        //by startsWith and not case sensitive (there are classes e.g. *.asymmetric.X509 and *.asymmetric.x509.PEMUtil)
        final List<String> packages = Stream.of(
                bouncycastleConfig.digests.orElse(Collections.emptyList()).stream().map(p -> "digest." + p),
                bouncycastleConfig.asymmetricCiphers.orElse(Collections.emptyList()).stream().map(p -> "asymmetric." + p),
                bouncycastleConfig.symmetricCiphers.orElse(Collections.emptyList()).stream().map(p -> "symmetric." + p))
                .flatMap(s -> s)
                .map(p -> "org.bouncycastle.jcajce.provider." + p.toLowerCase())
                .collect(Collectors.toList());

        String[] dtos = index.getKnownClasses().stream()
                .map(ci -> ci.name().toString())
                .filter(n -> {
                    String lowerCased = n.toLowerCase();
                    for (String s : packages) {
                        if (lowerCased.startsWith(s)) {
                            return true;
                        }
                    }
                    return false;
                })
                .sorted()
                .toArray(String[]::new);

        return new ReflectiveClassBuildItem(false, false, dtos);
    }

    @BuildStep
    IndexDependencyBuildItem registerBCDependencyForIndex() {
        return new IndexDependencyBuildItem("org.bouncycastle", "bcprov-jdk15on");
    }
}
