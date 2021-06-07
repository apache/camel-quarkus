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
package org.apache.camel.quarkus.component.crypto.deployment;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import org.apache.camel.quarkus.support.bouncycastle.deployment.CipherTransformationBuildItem;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.openpgp.PGPUtil;
import org.jboss.logging.Logger;

class CryptoProcessor {

    private static final Logger LOG = Logger.getLogger(CryptoProcessor.class);

    private static final String FEATURE = "camel-crypto";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem activeNativeSSLSupport() {
        return new ExtensionSslNativeSupportBuildItem(FEATURE);
    }

    @BuildStep
    CipherTransformationBuildItem registerReachableCipherTransformations() {
        List<String> cipherTransformations = new ArrayList<>();
        for (Field field : SymmetricKeyAlgorithmTags.class.getDeclaredFields()) {
            try {
                String algorithmName = PGPUtil.getSymmetricCipherName(field.getInt(null));
                if (algorithmName != null) {
                    String format = "Adding transformation '%s' to the CipherTransformationBuildItem produced by camel-quarkus-crypto";

                    // When using integrity packet, CFB mode is reachable
                    String cfbTransformation = algorithmName + "/CFB/NoPadding";
                    LOG.debugf(format, cfbTransformation);
                    cipherTransformations.add(cfbTransformation);

                    // When NOT using integrity packet, OpenPGPCFB mode is reachable
                    String openPgpCfbTransformation = algorithmName + "/OpenPGPCFB/NoPadding";
                    LOG.debugf(format, openPgpCfbTransformation);
                    cipherTransformations.add(openPgpCfbTransformation);
                }
            } catch (IllegalArgumentException | IllegalAccessException e) {
                // Ignoring inaccessible and non integer fields
            }
        }

        return new CipherTransformationBuildItem(cipherTransformations);
    }
}
