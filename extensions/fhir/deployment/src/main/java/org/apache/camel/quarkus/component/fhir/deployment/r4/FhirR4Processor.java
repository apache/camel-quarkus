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
package org.apache.camel.quarkus.component.fhir.deployment.r4;

import java.util.HashSet;
import java.util.Set;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.apache.camel.quarkus.component.fhir.FhirContextRecorder;
import org.apache.camel.quarkus.component.fhir.FhirFlags;

import static org.apache.camel.quarkus.component.fhir.deployment.FhirUtil.getInnerClasses;
import static org.apache.camel.quarkus.component.fhir.deployment.FhirUtil.getModelClasses;
import static org.apache.camel.quarkus.component.fhir.deployment.FhirUtil.getResourceDefinitions;

public class FhirR4Processor {
    private static final String FHIR_VERSION_PROPERTIES = "org/hl7/fhir/r4/model/fhirversion.properties";

    @BuildStep(onlyIf = FhirFlags.R4Enabled.class)
    R4PropertiesBuildItem properties(BuildProducer<NativeImageResourceBuildItem> resource) {
        resource.produce(new NativeImageResourceBuildItem(FHIR_VERSION_PROPERTIES));
        return new R4PropertiesBuildItem(FHIR_VERSION_PROPERTIES);
    }

    @BuildStep(onlyIf = FhirFlags.R4Enabled.class)
    @Record(ExecutionTime.STATIC_INIT)
    void recordContext(FhirContextRecorder fhirContextRecorder, BeanContainerBuildItem beanContainer,
            R4PropertiesBuildItem propertiesBuildItem) {
        fhirContextRecorder.createR4FhirContext(beanContainer.getValue(),
                getResourceDefinitions(propertiesBuildItem.getProperties()));
    }

    @BuildStep(onlyIf = FhirFlags.R4Enabled.class)
    void enableReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClass, R4PropertiesBuildItem buildItem) {
        Set<String> classes = new HashSet<>();
        classes.add(org.hl7.fhir.r4.model.DomainResource.class.getCanonicalName());
        classes.add(org.hl7.fhir.r4.model.Resource.class.getCanonicalName());
        classes.add(org.hl7.fhir.r4.model.BaseResource.class.getCanonicalName());
        classes.add(org.hl7.fhir.r4.model.Base.class.getCanonicalName());
        classes.addAll(getModelClasses(buildItem.getProperties()));
        classes.addAll(getInnerClasses(org.hl7.fhir.r4.model.Enumerations.class.getCanonicalName()));
        reflectiveClass
                .produce(new ReflectiveClassBuildItem(true, true, true, org.hl7.fhir.r4.model.Meta.class.getCanonicalName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, true,
                org.hl7.fhir.r4.model.MetadataResource.class.getCanonicalName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, classes.toArray(new String[0])));
    }
}
