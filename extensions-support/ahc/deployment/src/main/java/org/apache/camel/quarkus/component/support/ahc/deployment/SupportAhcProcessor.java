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
package org.apache.camel.quarkus.component.support.ahc.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;

class SupportAhcProcessor {

    private static final String FEATURE = "camel-support-ahc";
    private static final String[] RUNTIME_INITIALIZED_CLASSES = new String[] {
            "org.asynchttpclient.netty.channel.ChannelManager",
            "org.asynchttpclient.netty.request.NettyRequestSender",
            "org.asynchttpclient.RequestBuilderBase",
            "org.asynchttpclient.resolver.RequestHostnameResolver"
    };

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    NativeImageResourceBuildItem nativeImageResources() {
        return new NativeImageResourceBuildItem(
                "org/asynchttpclient/config/ahc-default.properties",
                "org/asynchttpclient/config/ahc.properties");
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(FEATURE);
    }

    @BuildStep
    void runtimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClass) {
        for (String className : RUNTIME_INITIALIZED_CLASSES) {
            runtimeInitializedClass
                    .produce(new RuntimeInitializedClassBuildItem(className));
        }
    }
}
