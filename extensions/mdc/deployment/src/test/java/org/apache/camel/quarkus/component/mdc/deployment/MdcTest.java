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
package org.apache.camel.quarkus.component.mdc.deployment;

import java.util.Set;

import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.apache.camel.mdc.MDCService;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class MdcTest {

    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.camel.mdc.enabled", "true")
            .overrideConfigKey("quarkus.camel.mdc.custom-exchange-headers", "head1,head2")
            .overrideConfigKey("quarkus.camel.mdc.custom-exchange-properties", "*")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Inject
    CamelContext context;

    @Test
    public void camelMdcServiceRegistryBeanNotNull() {
        Set<MDCService> mdcServices = context.getRegistry().findByType(MDCService.class);
        assertEquals(1, mdcServices.size());

        MDCService mdcSvc = mdcServices.iterator().next();
        assertInstanceOf(MDCService.class, mdcSvc);
        assertEquals("head1,head2", mdcSvc.getCustomHeaders());
        assertEquals("*", mdcSvc.getCustomProperties());
    }
}
