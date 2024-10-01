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
package org.apache.camel.quarkus.core.runtime;

import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.apache.camel.BindToRegistry;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spi.Registry;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class CamelBindToRegistryTest {
    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Inject
    Registry registry;

    @Test
    void bindBeansToRegistry() {
        assertNotNull(registry.lookupByName("ServiceA"));
        assertNotNull(registry.lookupByName("anotherServiceA"));
        assertNotNull(registry.lookupByName("serviceB"));
        assertNotNull(registry.lookupByName("ServiceC"));
        assertNotNull(registry.lookupByName("ServiceD"));
    }

    @BindToRegistry
    public static class ServiceA {
    }

    public static class ServiceB {
        @BindToRegistry("anotherServiceA")
        private ServiceA serviceA = new ServiceA();
    }

    @BindToRegistry
    public static class ServiceC {
        @BindToRegistry
        public ServiceB serviceB() {
            return new ServiceB();
        }
    }

    public static class Routes extends RouteBuilder {
        @Override
        public void configure() throws Exception {
            from("direct:start").log("Hello World");
        }

        @BindToRegistry
        public static class ServiceD {
        }
    }
}
