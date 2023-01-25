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
package org.apache.camel.quarkus.component.fhir.deployment.r5;

import java.util.HashSet;
import java.util.Set;

import ca.uhn.fhir.context.FhirContext;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import jakarta.inject.Singleton;
import org.apache.camel.quarkus.component.fhir.FhirContextRecorder;
import org.apache.camel.quarkus.component.fhir.FhirFlags;
import org.hl7.fhir.r5.model.Base;
import org.hl7.fhir.r5.model.BaseResource;
import org.hl7.fhir.r5.model.DomainResource;
import org.hl7.fhir.r5.model.Enumerations;
import org.hl7.fhir.r5.model.Meta;
import org.hl7.fhir.r5.model.MetadataResource;
import org.hl7.fhir.r5.model.Resource;

import static org.apache.camel.quarkus.component.fhir.deployment.FhirUtil.getInnerClasses;
import static org.apache.camel.quarkus.component.fhir.deployment.FhirUtil.getModelClasses;
import static org.apache.camel.quarkus.component.fhir.deployment.FhirUtil.getResourceDefinitions;

public class FhirR5Processor {
    private static final String FHIR_VERSION_PROPERTIES = "org/hl7/fhir/r5/model/fhirversion.properties";

    @BuildStep(onlyIf = FhirFlags.R5Enabled.class)
    R5PropertiesBuildItem fhirProperties() {
        return new R5PropertiesBuildItem(FHIR_VERSION_PROPERTIES);
    }

    @BuildStep(onlyIf = FhirFlags.R5Enabled.class)
    NativeImageResourceBuildItem nativeImageResources() {
        return new NativeImageResourceBuildItem(FHIR_VERSION_PROPERTIES);
    }

    @BuildStep(onlyIf = FhirFlags.R5Enabled.class)
    @Record(ExecutionTime.STATIC_INIT)
    SyntheticBeanBuildItem recordFhirContext(
            FhirContextRecorder recorder,
            R5PropertiesBuildItem propertiesBuildItem) {
        return SyntheticBeanBuildItem.configure(FhirContext.class)
                .scope(Singleton.class)
                .named("R5")
                .runtimeValue(recorder.createR5FhirContext(
                        getResourceDefinitions(propertiesBuildItem.getProperties())))
                .done();
    }

    @BuildStep(onlyIf = FhirFlags.R5Enabled.class)
    void registerForReflection(
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            R5PropertiesBuildItem propertiesBuildItem) {
        Set<String> classes = new HashSet<>();
        classes.add(DomainResource.class.getName());
        classes.add(Resource.class.getName());
        classes.add(BaseResource.class.getName());
        classes.add(Base.class.getName());
        classes.addAll(getModelClasses(propertiesBuildItem.getProperties()));
        classes.addAll(getInnerClasses(Enumerations.class.getName()));
        reflectiveClass
                .produce(new ReflectiveClassBuildItem(true, true, true, Meta.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, true,
                MetadataResource.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, classes.toArray(new String[0])));
    }
}
