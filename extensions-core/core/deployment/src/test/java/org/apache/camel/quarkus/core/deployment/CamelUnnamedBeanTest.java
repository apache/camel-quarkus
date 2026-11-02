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
package org.apache.camel.quarkus.core.deployment;

import java.util.Map;
import java.util.UUID;

import io.quarkus.test.QuarkusExtensionTest;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.engine.DefaultUnitOfWork;
import org.apache.camel.spi.UnitOfWork;
import org.apache.camel.spi.UnitOfWorkFactory;
import org.apache.camel.spi.UuidGenerator;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class CamelUnnamedBeanTest {

    @RegisterExtension
    static final QuarkusExtensionTest CONFIG = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Inject
    CamelContext context;

    @Test
    void unnamedBeanIsExposedByType() {
        Map<String, MyUnnamedBean> beans = context.getRegistry().findByTypeWithName(MyUnnamedBean.class);
        assertThat(beans).hasSize(1);
        // Unnamed beans are keyed by their bean identifier
        assertThat(beans.keySet()).doesNotContainNull();
        assertThat(beans.values()).allMatch(MyUnnamedBean.class::isInstance);
    }

    @Test
    void unnamedSpiBeanIsInstalledOnCamelContext() {
        // Camel installs SPI beans resolved through Registry#findByTypeWithName (DefaultConfigurationConfigurer)
        assertThat(context.getCamelContextExtension().getContextPlugin(UnitOfWorkFactory.class))
                .isInstanceOf(MyUnitOfWorkFactory.class);
    }

    @Test
    void namedAndUnnamedBeansOfSameTypeAreBothVisible() {
        Map<String, UuidGenerator> beans = context.getRegistry().findByTypeWithName(UuidGenerator.class);
        assertThat(beans).hasSize(2);
        assertThat(beans).containsKey("my-uuid-generator");
        assertThat(beans.values()).allMatch(MyUuidGenerator.class::isInstance);
    }

    @Produces
    public MyUnnamedBean createUnnamedBean() {
        return new MyUnnamedBean();
    }

    @Produces
    public UnitOfWorkFactory createUnitOfWorkFactory() {
        return new MyUnitOfWorkFactory();
    }

    @Produces
    @Named("my-uuid-generator")
    public UuidGenerator createNamedUuidGenerator() {
        return new MyUuidGenerator();
    }

    @Produces
    public UuidGenerator createUnnamedUuidGenerator() {
        return new MyUuidGenerator();
    }

    public static class MyUnnamedBean {
    }

    public static class MyUnitOfWorkFactory implements UnitOfWorkFactory {
        @Override
        public UnitOfWork createUnitOfWork(Exchange exchange) {
            return new DefaultUnitOfWork(exchange);
        }
    }

    public static class MyUuidGenerator implements UuidGenerator {
        @Override
        public String generateUuid() {
            return UUID.randomUUID().toString();
        }
    }
}
