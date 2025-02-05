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

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
@ConfigMapping(prefix = "quarkus.camel.fhir")
public interface FhirConfig {

    /**
     * Enable FHIR DSTU2 Specs in native mode.
     *
     * @asciidoclet
     */
    @WithName("enable-dstu2")
    @WithDefault("false")
    boolean enableDstu2();

    /**
     * Enable FHIR DSTU2_HL7ORG Specs in native mode.
     *
     * @asciidoclet
     */
    @WithName("enable-dstu2_hl7org")
    @WithDefault("false")
    boolean enableDstu2Hl7Org();

    /**
     * Enable FHIR DSTU2_1 Specs in native mode.
     *
     * @asciidoclet
     */
    @WithName("enable-dstu2_1")
    @WithDefault("false")
    boolean enableDstu2_1();

    /**
     * Enable FHIR DSTU3 Specs in native mode.
     *
     * @asciidoclet
     */
    @WithName("enable-dstu3")
    @WithDefault("false")
    boolean enableDstu3();

    /**
     * Enable FHIR R4 Specs in native mode.
     *
     * @asciidoclet
     */
    @WithName("enable-r4")
    @WithDefault("true")
    boolean enableR4();

    /**
     * Enable FHIR R5 Specs in native mode.
     *
     * @asciidoclet
     */
    @WithName("enable-r5")
    @WithDefault("false")
    boolean enableR5();
}
