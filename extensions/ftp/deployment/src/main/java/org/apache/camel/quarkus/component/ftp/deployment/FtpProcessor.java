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

import java.util.Arrays;
import java.util.List;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import org.apache.camel.component.file.remote.FtpConfiguration;
import org.apache.camel.component.file.remote.FtpsConfiguration;
import org.apache.camel.component.file.remote.RemoteFileConfiguration;
import org.apache.camel.component.file.remote.SftpConfiguration;
import org.apache.camel.quarkus.component.ftp.graal.AnyLocalAddress;

class FtpProcessor {

    private static final String FEATURE = "camel-ftp";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void registerForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false,
                RemoteFileConfiguration.class,
                FtpConfiguration.class,
                FtpsConfiguration.class,
                SftpConfiguration.class));

        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true,
                "com.jcraft.jsch.CipherNone",
                "com.jcraft.jsch.DHEC256",
                "com.jcraft.jsch.DHEC384",
                "com.jcraft.jsch.DHEC521",
                "com.jcraft.jsch.DHG1",
                "com.jcraft.jsch.DHG14",
                "com.jcraft.jsch.DHGEX",
                "com.jcraft.jsch.DHGEX256",
                "com.jcraft.jsch.jce.AES128CBC",
                "com.jcraft.jsch.jce.AES128CTR",
                "com.jcraft.jsch.jce.AES192CBC",
                "com.jcraft.jsch.jce.AES192CTR",
                "com.jcraft.jsch.jce.AES256CBC",
                "com.jcraft.jsch.jce.AES256CTR",
                "com.jcraft.jsch.jce.ARCFOUR",
                "com.jcraft.jsch.jce.ARCFOUR128",
                "com.jcraft.jsch.jce.ARCFOUR256",
                "com.jcraft.jsch.jce.BlowfishCBC",
                "com.jcraft.jsch.jce.DH",
                "com.jcraft.jsch.jce.ECDHN",
                "com.jcraft.jsch.jce.HMACMD5",
                "com.jcraft.jsch.jce.HMACMD596",
                "com.jcraft.jsch.jce.HMACSHA1",
                "com.jcraft.jsch.jce.HMACSHA1",
                "com.jcraft.jsch.jce.HMACSHA196",
                "com.jcraft.jsch.jce.HMACSHA256",
                "com.jcraft.jsch.jce.KeyPairGenDSA",
                "com.jcraft.jsch.jce.KeyPairGenECDSA",
                "com.jcraft.jsch.jce.KeyPairGenRSA",
                "com.jcraft.jsch.jce.MD5",
                "com.jcraft.jsch.jce.Random",
                "com.jcraft.jsch.jce.SHA1",
                "com.jcraft.jsch.jce.SHA256",
                "com.jcraft.jsch.jce.SHA384",
                "com.jcraft.jsch.jce.SHA512",
                "com.jcraft.jsch.jce.SignatureDSA",
                "com.jcraft.jsch.jce.SignatureECDSA256",
                "com.jcraft.jsch.jce.SignatureECDSA384",
                "com.jcraft.jsch.jce.SignatureECDSA521",
                "com.jcraft.jsch.jce.SignatureRSA",
                "com.jcraft.jsch.jce.TripleDESCBC",
                "com.jcraft.jsch.jce.TripleDESCTR",
                "com.jcraft.jsch.jcraft.Compression",
                "com.jcraft.jsch.jgss.GSSContextKrb5",
                "com.jcraft.jsch.UserAuthGSSAPIWithMIC",
                "com.jcraft.jsch.UserAuthKeyboardInteractive",
                "com.jcraft.jsch.UserAuthNone",
                "com.jcraft.jsch.UserAuthPassword",
                "com.jcraft.jsch.UserAuthPublicKey"));
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(FEATURE);
    }

    @BuildStep
    List<RuntimeInitializedClassBuildItem> runtimeInitializedClasses() {
        return Arrays.asList(
                new RuntimeInitializedClassBuildItem(AnyLocalAddress.class.getName()));
    }
}
