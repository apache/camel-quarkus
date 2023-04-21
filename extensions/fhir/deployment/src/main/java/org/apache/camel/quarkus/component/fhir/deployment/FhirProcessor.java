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

import java.lang.reflect.Modifier;
import java.util.function.BooleanSupplier;

import ca.uhn.fhir.rest.client.apache.ApacheRestfulClientFactory;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.util.jar.DependencyLogImpl;
import ca.uhn.fhir.validation.schematron.SchematronBaseValidator;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalApplicationArchiveMarkerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.gizmo.MethodCreator;
import org.apache.camel.quarkus.component.fhir.IsSchematronAbsent;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

final class FhirProcessor {
    private static final String FEATURE = "camel-fhir";
    private static final String RESTFUL_SERVER_UTILS_CLASS_NAME = "ca.uhn.fhir.rest.server.RestfulServerUtils";
    private static final String[] INTERCEPTOR_CLASSES = {
            IClientInterceptor.class.getName(),
    };

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(FEATURE);
    }

    @BuildStep()
    NativeImageResourceBundleBuildItem hapiMessages() {
        return new NativeImageResourceBundleBuildItem("ca.uhn.fhir.i18n.hapi-messages");
    }

    @BuildStep
    AdditionalApplicationArchiveMarkerBuildItem hl7ArchiveMarker() {
        return new AdditionalApplicationArchiveMarkerBuildItem("org/hl7/fhir");
    }

    @BuildStep
    AdditionalApplicationArchiveMarkerBuildItem hapiArchiveMarker() {
        return new AdditionalApplicationArchiveMarkerBuildItem("ca/uhn/fhir");
    }

    @BuildStep()
    void registerForReflection(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {

        IndexView index = combinedIndex.getIndex();
        index.getAllKnownSubclasses(DotName.createSimple(BaseServerResponseException.class.getName()))
                .stream()
                .map(classInfo -> ReflectiveClassBuildItem.builder(classInfo.name().toString())
                        .build())
                .forEach(reflectiveClass::produce);

        String[] clientInterceptors = index.getAllKnownImplementors(DotName.createSimple(IClientInterceptor.class.getName()))
                .stream()
                .map(classInfo -> classInfo.name().toString())
                .toArray(String[]::new);

        reflectiveClass.produce(ReflectiveClassBuildItem.builder(clientInterceptors).methods().build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(INTERCEPTOR_CLASSES).methods().build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(DependencyLogImpl.class)
                .fields().build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(ApacheRestfulClientFactory.class)
                .methods().fields().build());
    }

    @BuildStep(onlyIfNot = IsSchematronAbsent.class)
    void registerSchematronForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(SchematronBaseValidator.class)
                .fields().build());
    }

    @BuildStep(onlyIf = { NativeBuild.class, IsFhirServerAbsent.class })
    void generateRestfulServerUtils(BuildProducer<GeneratedClassBuildItem> generatedClass) {
        // Avoid having redundant hapi-fhir-server on the classpath and generate RestfulServerUtils.createEtag to satisfy native compilation.
        try (ClassCreator classCreator = ClassCreator.builder()
                .className(RESTFUL_SERVER_UTILS_CLASS_NAME)
                .classOutput(new GeneratedClassGizmoAdaptor(generatedClass, false))
                .setFinal(true)
                .superClass(Object.class.getName())
                .build()) {

            /*
             * Original implementation of createETag is:
             *
             * 	public static String createEtag(String theVersionId) {
             * 		return "W/\"" + theVersionId + '"';
             *  }
             */
            try (MethodCreator methodCreator = classCreator.getMethodCreator("createEtag", String.class, String.class)) {
                methodCreator.setModifiers(Modifier.PUBLIC | Modifier.STATIC);

                Gizmo.StringBuilderGenerator stringBuilder = Gizmo.newStringBuilder(methodCreator);
                stringBuilder.append("W/");
                stringBuilder.append('"');
                stringBuilder.append(methodCreator.getMethodParam(0));
                stringBuilder.append('"');

                methodCreator.returnValue(stringBuilder.callToString());
            }
        }
    }

    static final class IsFhirServerAbsent implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            try {
                Class.forName(RESTFUL_SERVER_UTILS_CLASS_NAME);
                return false;
            } catch (ClassNotFoundException e) {
                return true;
            }
        }
    }
}
