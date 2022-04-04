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
package org.apache.camel.quarkus.component.fhir.it;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;

import ca.uhn.fhir.context.FhirContext;
import org.apache.camel.quarkus.component.fhir.FhirFlags;

@ApplicationScoped
public class FhirDstu2RouteBuilder extends AbstractFhirRouteBuilder {

    private static final Boolean ENABLED = new FhirFlags.Dstu2Enabled().getAsBoolean();

    @Inject
    @Named("DSTU2")
    Instance<FhirContext> fhirContextInstance;

    @Override
    String getFhirVersion() {
        return "dstu2";
    }

    @Override
    FhirContext getFhirContext() {
        return fhirContextInstance.get();
    }

    @Override
    boolean isEnabled() {
        return ENABLED;
    }
}
