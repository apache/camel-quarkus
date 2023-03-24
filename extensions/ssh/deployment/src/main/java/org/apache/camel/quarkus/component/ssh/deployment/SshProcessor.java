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
package org.apache.camel.quarkus.component.ssh.deployment;

import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Arrays;

import javax.crypto.KeyAgreement;
import javax.crypto.Mac;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.apache.sshd.common.channel.ChannelListener;
import org.apache.sshd.common.forward.PortForwardingEventListener;
import org.apache.sshd.common.io.nio2.Nio2ServiceFactoryFactory;
import org.apache.sshd.common.session.SessionListener;

class SshProcessor {

    private static final String FEATURE = "camel-ssh";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void registerForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        reflectiveClasses.produce(
                ReflectiveClassBuildItem.builder(KeyPairGenerator.class,
                        KeyAgreement.class,
                        KeyFactory.class,
                        Signature.class,
                        Mac.class).methods().build());
        reflectiveClasses.produce(
                ReflectiveClassBuildItem.builder(Nio2ServiceFactoryFactory.class).build());
    }

    @BuildStep
    NativeImageResourceBuildItem nativeImageResourceBuildItem() {
        return new NativeImageResourceBuildItem("META-INF/services/org.apache.sshd.common.io.IoServiceFactoryFactory");
    }

    @BuildStep
    void sessionProxy(BuildProducer<NativeImageProxyDefinitionBuildItem> proxiesProducer) {
        for (String s : Arrays.asList(
                SessionListener.class.getName(),
                ChannelListener.class.getName(),
                PortForwardingEventListener.class.getName())) {
            proxiesProducer.produce(new NativeImageProxyDefinitionBuildItem(s));
        }
    }

}
