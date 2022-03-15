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

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "camel.fhir", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public final class FhirConfig {

    /**
     * Enable FHIR DSTU2 Specs in native mode.
     */
    @ConfigItem(name = "enable-dstu2", defaultValue = "true")
    public boolean enableDstu2;

    /**
     * Enable FHIR DSTU3 Specs in native mode.
     */
    @ConfigItem(name = "enable-dstu3", defaultValue = "true")
    public boolean enableDstu3;

    /**
     * Enable FHIR R4 Specs in native mode.
     */
    @ConfigItem(name = "enable-r4", defaultValue = "true")
    public boolean enableR4;

    /**
     * Enable FHIR R5 Specs in native mode.
     */
    @ConfigItem(name = "enable-r5", defaultValue = "true")
    public boolean enableR5;
}
