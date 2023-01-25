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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import io.quarkus.test.QuarkusUnitTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.camel.Component;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spi.DataFormat;
import org.apache.camel.spi.Language;
import org.apache.camel.spi.Registry;
import org.apache.camel.support.DefaultComponent;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class CamelRegistryTest {
    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Inject
    Registry registry;

    @Test
    public void testLookupRoutes() {
        assertThat(registry.findByType(RoutesBuilder.class)).hasSize(2);
        assertThat(registry.lookupByNameAndType("my-route", RoutesBuilder.class)).isNotNull();
        assertThat(registry.lookupByNameAndType("my-route-produced", RoutesBuilder.class)).isNotNull();
    }

    @Test
    public void testLookupCustomServices() {
        assertThat(registry.lookupByNameAndType("my-df", DataFormat.class)).isNotNull();
        assertThat(registry.lookupByNameAndType("my-language", Language.class)).isNotNull();
        assertThat(registry.lookupByNameAndType("my-component", Component.class)).isNotNull();
        assertThat(registry.lookupByNameAndType("my-predicate", Predicate.class)).isNotNull();
        assertThat(registry.lookupByNameAndType("my-processor", Processor.class)).isNotNull();
    }

    @Test
    public void testLookupByName() {
        assertThat(registry.lookupByName("bean-1")).isInstanceOfSatisfying(String.class, s -> assertThat(s).isEqualTo("a"));
        assertThat(registry.lookupByName("bean-2")).isInstanceOfSatisfying(String.class, s -> assertThat(s).isEqualTo("b"));
        assertThat(registry.lookupByNameAndType("bean-1", String.class)).isEqualTo("a");
        assertThat(registry.lookupByNameAndType("bean-2", String.class)).isEqualTo("b");
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
    }

    @Named("my-route")
    @ApplicationScoped
    public static class MyCDIRoute extends RouteBuilder {
        @Override
        public void configure() throws Exception {
        }
    }

    @ApplicationScoped
    public static class MyCDIProducer {
        @Named("my-route-produced")
        @Produces
        public RoutesBuilder routes() {
            return new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                }
            };
        }

        @Named("my-predicate")
        @Produces
        public Predicate predicate() {
            return e -> false;
        }

        @Named("my-processor")
        @Produces
        public Processor processor() {
            return e -> {
            };
        }
    }

    @Named("my-df")
    @ApplicationScoped
    public static class MyDataFormat implements DataFormat {
        @Override
        public void marshal(Exchange exchange, Object graph, OutputStream stream) throws Exception {
        }

        @Override
        public Object unmarshal(Exchange exchange, InputStream stream) throws Exception {
            return null;
        }

        @Override
        public void start() {
        }

        @Override
        public void stop() {
        }
    }

    @Named("my-language")
    @ApplicationScoped
    public static class MyLanguage implements Language {
        @Override
        public Predicate createPredicate(String expression) {
            return null;
        }

        @Override
        public Expression createExpression(String expression) {
            return null;
        }
    }

    @Named("my-component")
    @ApplicationScoped
    public static class MyComponent extends DefaultComponent {
        @Override
        protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
            return null;
        }
    }
}
