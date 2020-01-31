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

import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;

import ca.uhn.fhir.context.FhirContext;

@Singleton
public class FhirContextProducers {

    private volatile FhirContext dstu2;
    private volatile FhirContext dstu3;
    private volatile FhirContext r4;
    private volatile FhirContext r5;

    public void setDstu2(FhirContext dstu2) {
        this.dstu2 = dstu2;
    }

    public void setDstu3(FhirContext dstu3) {
        this.dstu3 = dstu3;
    }

    public void setR4(FhirContext r4) {
        this.r4 = r4;
    }

    public void setR5(FhirContext r5) {
        this.r5 = r5;
    }

    @Singleton
    @Produces
    @Named("DSTU2")
    FhirContext dstu2() {
        return this.dstu2;
    }

    @Singleton
    @Produces
    @Named("DSTU3")
    FhirContext dstu3() {
        return this.dstu3;
    }

    @Singleton
    @Produces
    @Named("R4")
    FhirContext r4() {
        return this.r4;
    }

    @Singleton
    @Produces
    @Named("R5")
    FhirContext r5() {
        return this.r5;
    }
}
