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
package org.apache.camel.quarkus.component.soap.deployment;

import javax.xml.ws.WebFault;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

class SoapProcessor {

    private static final String FEATURE = "camel-soap";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    NativeImageResourceBuildItem nativeImageResources() {
        return new NativeImageResourceBuildItem("soap.xsd", "soap12.xsd", "xml.xsd");
    }

    @BuildStep
    void serviceProviders(BuildProducer<ServiceProviderBuildItem> serviceProvider) {
        String[] soapVersions = new String[] { "1_1", "1_2" };
        for (String version : soapVersions) {
            serviceProvider.produce(
                    new ServiceProviderBuildItem(
                            "javax.xml.soap.MessageFactory",
                            "com.sun.xml.messaging.saaj.soap.ver" + version + ".SOAPMessageFactory" + version + "Impl"));

            serviceProvider.produce(
                    new ServiceProviderBuildItem(
                            "javax.xml.soap.SOAPFactory",
                            "com.sun.xml.messaging.saaj.soap.ver" + version + ".SOAPFactory" + version + "Impl"));
        }

        serviceProvider.produce(
                new ServiceProviderBuildItem(
                        "javax.xml.soap.SOAPConnectionFactory",
                        "com.sun.xml.messaging.saaj.client.p2p.HttpSOAPConnectionFactory"));

        serviceProvider.produce(
                new ServiceProviderBuildItem(
                        "javax.xml.soap.SAAJMetaFactory",
                        "com.sun.xml.messaging.saaj.soap.SAAJMetaFactoryImpl"));
    }

    @BuildStep
    void registerForReflection(CombinedIndexBuildItem combinedIndex, BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        IndexView index = combinedIndex.getIndex();

        // Required for SOAP fault marshal / unmarshal
        index.getAnnotations(DotName.createSimple(WebFault.class.getName()))
                .stream()
                .map(annotationInstance -> annotationInstance.target().asClass())
                .map(classInfo -> new ReflectiveClassBuildItem(true, false, classInfo.name().toString()))
                .forEach(reflectiveClass::produce);

        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, Exception.class));
    }

}
