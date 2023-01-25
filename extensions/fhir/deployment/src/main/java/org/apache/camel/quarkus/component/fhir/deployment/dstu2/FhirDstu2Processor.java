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
package org.apache.camel.quarkus.component.fhir.deployment.dstu2;

import java.util.HashSet;
import java.util.Set;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.dstu2.resource.BaseResource;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import jakarta.inject.Singleton;
import org.apache.camel.quarkus.component.fhir.FhirContextRecorder;
import org.apache.camel.quarkus.component.fhir.FhirFlags;

import static org.apache.camel.quarkus.component.fhir.deployment.FhirUtil.getModelClasses;
import static org.apache.camel.quarkus.component.fhir.deployment.FhirUtil.getResourceDefinitions;

public class FhirDstu2Processor {

    private static final String FHIR_VERSION_PROPERTIES = "ca/uhn/fhir/model/dstu2/fhirversion.properties";

    @BuildStep(onlyIf = FhirFlags.Dstu2Enabled.class)
    Dstu2PropertiesBuildItem fhirProperties() {
        return new Dstu2PropertiesBuildItem(FHIR_VERSION_PROPERTIES);
    }

    @BuildStep(onlyIf = FhirFlags.Dstu2Enabled.class)
    NativeImageResourceBuildItem nativeImageResources() {
        return new NativeImageResourceBuildItem(FHIR_VERSION_PROPERTIES);
    }

    @BuildStep(onlyIf = FhirFlags.Dstu2Enabled.class)
    @Record(ExecutionTime.STATIC_INIT)
    SyntheticBeanBuildItem recordFhirContext(
            FhirContextRecorder recorder,
            Dstu2PropertiesBuildItem propertiesBuildItem) {
        return SyntheticBeanBuildItem.configure(FhirContext.class)
                .scope(Singleton.class)
                .named("DSTU2")
                .runtimeValue(recorder.createDstu2FhirContext(
                        getResourceDefinitions(propertiesBuildItem.getProperties())))
                .done();
    }

    @BuildStep(onlyIf = FhirFlags.Dstu2Enabled.class)
    void registerForReflection(
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            Dstu2PropertiesBuildItem propertiesBuildItem,
            CombinedIndexBuildItem combinedIndex) {
        Set<String> classes = new HashSet<>();
        classes.add(BaseResource.class.getCanonicalName());
        classes.addAll(getModelClasses(propertiesBuildItem.getProperties()));

        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, classes.toArray(new String[0])));

        String[] dstu2Enums = combinedIndex.getIndex()
                .getKnownClasses()
                .stream()
                .map(classInfo -> classInfo.toString())
                .filter(className -> className.startsWith("ca.uhn.fhir.model.dstu2.valueset"))
                .toArray(String[]::new);

        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, dstu2Enums));
    }
}
