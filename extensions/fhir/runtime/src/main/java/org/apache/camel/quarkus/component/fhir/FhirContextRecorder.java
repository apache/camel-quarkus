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
package org.apache.camel.quarkus.component.fhir;

import java.util.Collection;

import ca.uhn.fhir.context.FhirContext;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class FhirContextRecorder {

    public RuntimeValue<FhirContext> createDstu2FhirContext(BeanContainer container, Collection<String> resourceDefinitions) {
        FhirContext fhirContext = FhirContext.forDstu2();
        initContext(resourceDefinitions, fhirContext);
        container.instance(FhirContextProducers.class).setDstu2(fhirContext);
        return new RuntimeValue<>(fhirContext);
    }

    public RuntimeValue<FhirContext> createDstu3FhirContext(BeanContainer container, Collection<String> resourceDefinitions) {
        FhirContext fhirContext = FhirContext.forDstu3();
        initContext(resourceDefinitions, fhirContext);
        container.instance(FhirContextProducers.class).setDstu3(fhirContext);
        return new RuntimeValue<>(fhirContext);
    }

    public RuntimeValue<FhirContext> createR4FhirContext(BeanContainer container, Collection<String> resourceDefinitions) {
        FhirContext fhirContext = FhirContext.forR4();
        initContext(resourceDefinitions, fhirContext);
        container.instance(FhirContextProducers.class).setR4(fhirContext);
        return new RuntimeValue<>(fhirContext);
    }

    public RuntimeValue<FhirContext> createR5FhirContext(BeanContainer container, Collection<String> resourceDefinitions) {
        FhirContext fhirContext = FhirContext.forR5();
        initContext(resourceDefinitions, fhirContext);
        container.instance(FhirContextProducers.class).setR5(fhirContext);
        return new RuntimeValue<>(fhirContext);
    }

    private void initContext(Collection<String> resourceDefinitions, FhirContext fhirContext) {
        // force init
        fhirContext.getElementDefinitions();
        for (String resourceDefinition : resourceDefinitions) {
            fhirContext.getResourceDefinition(resourceDefinition);
        }
    }
}
