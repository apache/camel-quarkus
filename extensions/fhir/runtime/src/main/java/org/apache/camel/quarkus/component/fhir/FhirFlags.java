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

import java.util.function.BooleanSupplier;

import org.eclipse.microprofile.config.ConfigProvider;

public final class FhirFlags {
    private FhirFlags() {
    }

    public static final class Dstu2Enabled implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            return ConfigProvider.getConfig().getOptionalValue("quarkus.camel.fhir.enable-dstu2", Boolean.class)
                    .orElse(Boolean.FALSE);
        }
    }

    public static final class Dstu2Hl7OrgEnabled implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            return ConfigProvider.getConfig().getOptionalValue("quarkus.camel.fhir.enable-dstu2_hl7org", Boolean.class)
                    .orElse(Boolean.FALSE);
        }
    }

    public static final class Dstu2_1Enabled implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            return ConfigProvider.getConfig().getOptionalValue("quarkus.camel.fhir.enable-dstu2_1", Boolean.class)
                    .orElse(Boolean.FALSE);
        }
    }

    public static final class Dstu3Enabled implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            return ConfigProvider.getConfig().getOptionalValue("quarkus.camel.fhir.enable-dstu3", Boolean.class)
                    .orElse(Boolean.TRUE);
        }
    }

    public static final class R4Enabled implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            return ConfigProvider.getConfig().getOptionalValue("quarkus.camel.fhir.enable-r4", Boolean.class)
                    .orElse(Boolean.TRUE);
        }
    }

    public static final class R5Enabled implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            return ConfigProvider.getConfig().getOptionalValue("quarkus.camel.fhir.enable-r5", Boolean.class)
                    .orElse(Boolean.FALSE);
        }
    }
}
