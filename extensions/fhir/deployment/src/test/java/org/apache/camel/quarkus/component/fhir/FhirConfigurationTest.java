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

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableBean;
import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class FhirConfigurationTest {

    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.camel.fhir.enable-dstu2", "true")
            .overrideConfigKey("quarkus.camel.fhir.enable-dstu2_hl7org", "true")
            .overrideConfigKey("quarkus.camel.fhir.enable-dstu2_1", "true")
            .overrideConfigKey("quarkus.camel.fhir.enable-dstu3", "true")
            .overrideConfigKey("quarkus.camel.fhir.enable-r4", "true")
            .overrideConfigKey("quarkus.camel.fhir.enable-r5", "true")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Test
    void allModelsEnabled() {
        Assertions.assertNotNull(getFhirContextBean("DSTU2"));
        Assertions.assertNotNull(getFhirContextBean("DSTU2_1"));
        Assertions.assertNotNull(getFhirContextBean("DSTU2_HL7ORG"));
        Assertions.assertNotNull(getFhirContextBean("DSTU3"));
        Assertions.assertNotNull(getFhirContextBean("R4"));
        Assertions.assertNotNull(getFhirContextBean("R5"));
    }

    private InjectableBean<?> getFhirContextBean(String beanName) {
        return Arc.container().namedBean(beanName);
    }
}
