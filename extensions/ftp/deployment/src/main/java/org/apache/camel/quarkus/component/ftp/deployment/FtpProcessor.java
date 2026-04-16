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
package org.apache.camel.quarkus.component.ftp.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

class FtpProcessor {

    private static final String FEATURE = "camel-ftp";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ReflectiveClassBuildItem registerJSchCertificateClasses() {
        // JSch OpenSSH certificate support classes for @cert-authority parsing in known_hosts
        // The quarkus-jsch reflection config is missing several classes that JSch loads dynamically.
        return ReflectiveClassBuildItem.builder(
                "com.jcraft.jsch.KeyPairRSA",
                "com.jcraft.jsch.KeyPairECDSA",
                "com.jcraft.jsch.KeyPairEd25519",
                "com.jcraft.jsch.KeyPairEd448",
                "com.jcraft.jsch.KeyPairDSA",
                "com.jcraft.jsch.SignatureRSA",
                "com.jcraft.jsch.SignatureECDSA",
                "com.jcraft.jsch.jce.SignatureEd25519")
                .build();
    }
}
