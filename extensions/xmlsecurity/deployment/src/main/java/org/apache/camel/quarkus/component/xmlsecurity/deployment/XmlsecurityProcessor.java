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
package org.apache.camel.quarkus.component.xmlsecurity.deployment;

import java.util.stream.Stream;

import javax.crypto.spec.GCMParameterSpec;
import javax.xml.crypto.dsig.spec.XPathType;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageSecurityProviderBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeReinitializedClassBuildItem;
import org.apache.jcp.xml.dsig.internal.dom.XMLDSigRI;
import org.apache.xml.security.c14n.CanonicalizerSpi;
import org.apache.xml.security.stax.ext.XMLSecurityConstants;
import org.apache.xml.security.transforms.TransformSpi;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

class XmlsecurityProcessor {

    private static final String FEATURE = "camel-xmlsecurity";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(FEATURE);
    }

    @BuildStep
    IndexDependencyBuildItem indexDependencies() {
        return new IndexDependencyBuildItem("org.apache.santuario", "xmlsec");
    }

    @BuildStep
    void registerForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClass, CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getIndex();

        Stream.of(CanonicalizerSpi.class, TransformSpi.class)
                .map(aClass -> aClass.getName())
                .map(DotName::createSimple)
                .flatMap(dotName -> index.getAllKnownSubclasses(dotName).stream())
                .map(classInfo -> classInfo.name().toString())
                .map(className -> new ReflectiveClassBuildItem(false, false, className))
                .forEach(reflectiveClass::produce);

        Stream.of(GCMParameterSpec.class.getName(), XPathType[].class.getName())
                .map(className -> new ReflectiveClassBuildItem(false, false, className))
                .forEach(reflectiveClass::produce);
    }

    @BuildStep
    void runtimeReinitializedClasses(BuildProducer<RuntimeReinitializedClassBuildItem> runtimeReinitializedClasses) {
        Stream.of(
                /* XMLSecurityConstants has a SecureRandom field initialized in a static initializer */
                XMLSecurityConstants.class.getName())
                .map(RuntimeReinitializedClassBuildItem::new)
                .forEach(runtimeReinitializedClasses::produce);
    }

    @BuildStep
    NativeImageSecurityProviderBuildItem saslSecurityProvider() {
        return new NativeImageSecurityProviderBuildItem(XMLDSigRI.class.getName());
    }
}
