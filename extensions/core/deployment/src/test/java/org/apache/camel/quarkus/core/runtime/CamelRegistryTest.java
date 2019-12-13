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

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import io.quarkus.test.QuarkusUnitTest;
import org.apache.camel.BeanInject;
import org.apache.camel.BindToRegistry;
import org.apache.camel.Processor;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spi.Registry;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class CamelRegistryTest {
    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(BeanProducer.class, MyRoute.class, MyCDIRoute.class, MyCDIProducer.class));

    @Inject
    Registry registry;

    @Test
    public void testLookupRoutes() {
        // MyCDIRoute
        // MyCDIProducer::routes
        assertThat(registry.findByType(RoutesBuilder.class))
                .hasSize(2);
    }

    @Test
    public void testCamelDI() {
        assertThat(registry.findByType(MyCDIRoute.class))
                .isNotEmpty()
                .first().hasFieldOrPropertyWithValue("bean1", "a");
    }

    @Test
    public void testLookupByName() {
        assertThat(registry.lookupByName("bean-1")).isInstanceOfSatisfying(String.class, s -> assertThat(s).isEqualTo("a"));
        assertThat(registry.lookupByName("bean-2")).isInstanceOfSatisfying(String.class, s -> assertThat(s).isEqualTo("b"));
        assertThat(registry.lookupByNameAndType("bean-1", String.class)).isEqualTo("a");
        assertThat(registry.lookupByNameAndType("bean-2", String.class)).isEqualTo("b");
        assertThat(registry.lookupByName("myProcessor")).isInstanceOf(Processor.class);
    }

    @Test
    public void testFindByType() {
        assertThat(registry.findByType(String.class)).containsOnly("a", "b");
        assertThat(registry.findByTypeWithName(String.class))
                .containsEntry("bean-1", "a")
                .containsEntry("bean-2", "b");
    }

    @ApplicationScoped
    public static class BeanProducer {
        @Named("bean-1")
        @Produces
        public String bean1() {
            return "a";
        }

        @Named("bean-2")
        @Produces
        public String bean2() {
            return "b";
        }
    }

    public static class MyRoute extends RouteBuilder {
        @Override
        public void configure() throws Exception {
        }

        @BindToRegistry
        public Processor myProcessor() {
            return e -> {
            };
        }
    }

    @ApplicationScoped
    public static class MyCDIRoute extends RouteBuilder {
        @BeanInject("bean-1")
        String bean1;

        @Override
        public void configure() throws Exception {
        }
    }

    @ApplicationScoped
    public static class MyCDIProducer {
        @Produces
        public RoutesBuilder routes() {
            return new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                }
            };
        }
    }
}
