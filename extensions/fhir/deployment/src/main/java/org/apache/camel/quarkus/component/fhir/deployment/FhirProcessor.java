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
package org.apache.camel.quarkus.component.fhir.deployment;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import ca.uhn.fhir.model.dstu2.FhirDstu2;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.apache.camel.component.fhir.FhirCapabilitiesEndpointConfiguration;
import org.apache.camel.component.fhir.FhirConfiguration;
import org.apache.camel.component.fhir.FhirCreateEndpointConfiguration;
import org.apache.camel.component.fhir.FhirDeleteEndpointConfiguration;
import org.apache.camel.component.fhir.FhirHistoryEndpointConfiguration;
import org.apache.camel.component.fhir.FhirLoadPageEndpointConfiguration;
import org.apache.camel.component.fhir.FhirMetaEndpointConfiguration;
import org.apache.camel.component.fhir.FhirOperationEndpointConfiguration;
import org.apache.camel.component.fhir.FhirPatchEndpointConfiguration;
import org.apache.camel.component.fhir.FhirReadEndpointConfiguration;
import org.apache.camel.component.fhir.FhirSearchEndpointConfiguration;
import org.apache.camel.component.fhir.FhirTransactionEndpointConfiguration;
import org.apache.camel.component.fhir.FhirUpdateEndpointConfiguration;
import org.apache.camel.component.fhir.FhirValidateEndpointConfiguration;
import org.apache.camel.quarkus.component.fhir.FhirFlags;
import org.hl7.fhir.dstu3.hapi.ctx.FhirDstu3;
import org.hl7.fhir.dstu3.model.Enumerations;

class FhirProcessor {
    private static final String FEATURE = "camel-fhir";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(FEATURE);
    }


    @BuildStep()
    ReflectiveClassBuildItem fhirEndpointConfiguration() {
        return new ReflectiveClassBuildItem(true, true,
                FhirCreateEndpointConfiguration.class,
                FhirCapabilitiesEndpointConfiguration.class,
                FhirDeleteEndpointConfiguration.class,
                FhirHistoryEndpointConfiguration.class,
                FhirLoadPageEndpointConfiguration.class,
                FhirMetaEndpointConfiguration.class,
                FhirOperationEndpointConfiguration.class,
                FhirPatchEndpointConfiguration.class,
                FhirReadEndpointConfiguration.class,
                FhirSearchEndpointConfiguration.class,
                FhirTransactionEndpointConfiguration.class,
                FhirUpdateEndpointConfiguration.class,
                FhirValidateEndpointConfiguration.class,
                FhirConfiguration.class);
    }

    @BuildStep()
    NativeImageResourceBundleBuildItem hapiMessages() {
        return new NativeImageResourceBundleBuildItem("ca.uhn.fhir.i18n.hapi-messages");
    }

    @BuildStep
    ReflectiveClassBuildItem xmlOutputFactory() {
        return new ReflectiveClassBuildItem(false, false,
                "com.sun.xml.internal.stream.XMLOutputFactoryImpl");
    }

    @BuildStep(applicationArchiveMarkers = {"org/hl7/fhir", "ca/uhn/fhir"})
    void processFhir(BuildProducer<ReflectiveClassBuildItem> reflectiveClass, BuildProducer<NativeImageResourceBundleBuildItem> resource) {
        Set<String> classes = new HashSet<>();
        classes.add("ca.uhn.fhir.rest.client.apache.ApacheRestfulClientFactory");
        classes.add("ca.uhn.fhir.validation.schematron.SchematronBaseValidator");
        classes.add("org.apache.commons.logging.impl.LogFactoryImpl");
        classes.add("org.apache.commons.logging.LogFactory");
        classes.add("org.apache.commons.logging.impl.Jdk14Logger");
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, true, classes.toArray(new String[0])));
    }

    @BuildStep(onlyIf = FhirFlags.Dstu2Enabled.class, applicationArchiveMarkers = {"org/hl7/fhir", "ca/uhn/fhir"})
    void processDstu2(BuildProducer<ReflectiveClassBuildItem> reflectiveClass, BuildProducer<NativeImageResourceBuildItem> resource) {
        Set<String> classes = new HashSet<>();
        classes.add(FhirDstu2.class.getCanonicalName());
        classes.addAll(getModelClasses("/ca/uhn/fhir/model/dstu2/fhirversion.properties"));

        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, true, classes.toArray(new String[0])));
        resource.produce(new NativeImageResourceBuildItem("ca/uhn/fhir/model/dstu2/fhirversion.properties"));
    }

    @BuildStep(onlyIf = FhirFlags.Dstu3Enabled.class, applicationArchiveMarkers = {"org/hl7/fhir", "ca/uhn/fhir"})
    void processDstu3(BuildProducer<ReflectiveClassBuildItem> reflectiveClass, BuildProducer<NativeImageResourceBuildItem> resource) {
        Set<String> classes = new HashSet<>();
        classes.add(FhirDstu3.class.getCanonicalName());
        classes.addAll(getModelClasses("/org/hl7/fhir/dstu3/model/fhirversion.properties"));
        classes.addAll(getInnerClasses(Enumerations.class.getCanonicalName()));

        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, true, classes.toArray(new String[0])));
        resource.produce(new NativeImageResourceBuildItem("org/hl7/fhir/dstu3/model/fhirversion.properties"));
    }

    @BuildStep(onlyIf = FhirFlags.R4Enabled.class, applicationArchiveMarkers = {"org/hl7/fhir", "ca/uhn/fhir"})
    void processR4(BuildProducer<ReflectiveClassBuildItem> reflectiveClass, BuildProducer<NativeImageResourceBuildItem> resource) {
        Set<String> classes = new HashSet<>();
        classes.add("org.hl7.fhir.r4.hapi.ctx.FhirR4");
        classes.addAll(getModelClasses("/org/hl7/fhir/r4/model/fhirversion.properties"));
        classes.addAll(getInnerClasses(org.hl7.fhir.r4.model.Enumerations.class.getCanonicalName()));

        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, true, classes.toArray(new String[0])));
        resource.produce(new NativeImageResourceBuildItem("org/hl7/fhir/r4/model/fhirversion.properties"));
    }

    private Collection<String> getModelClasses(String model) {
        try (InputStream str = FhirDstu3.class.getResourceAsStream(model)) {
            Properties prop = new Properties();
            prop.load(str);
            return getInnerClasses(prop.values().toArray(new String[0]));
        } catch (Exception e) {
            throw new RuntimeException("Please ensure FHIR is on the classpath", e);
        }
    }

    private Collection<String> getInnerClasses(String... classList) {
        try {
            Set<String> classes = new HashSet<>();
            for (Object value : classList) {
                String clazz = (String) value;
                final Class[] parent = Class.forName(clazz).getClasses();
                for (Class aClass : parent) {
                    String name = aClass.getName();
                    classes.add(name);
                }
                classes.add(clazz);
            }
            return classes;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Please ensure FHIR is on the classpath", e);
        }
    }
}
