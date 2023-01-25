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

import java.util.Set;

import io.quarkus.test.QuarkusUnitTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.camel.CamelContext;
import org.apache.camel.NamedNode;
import org.apache.camel.Processor;
import org.apache.camel.spi.InterceptStrategy;
import org.apache.camel.spi.Registry;
import org.apache.camel.spi.annotations.Component;
import org.apache.camel.spi.annotations.Dataformat;
import org.apache.camel.spi.annotations.Language;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class CamelBeansUnremovableTest {

    static final String RESSOURCE_PATH = "org/apache/camel/quarkus/core/runtime/"
            + CamelBeansUnremovableTest.class.getSimpleName();

    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("custom-component.json", RESSOURCE_PATH + "/custom-component.json")
                    .addAsResource("custom-dataformat.json", RESSOURCE_PATH + "/custom-dataformat.json")
                    .addAsResource("custom-language.json", RESSOURCE_PATH + "/custom-language.json"));

    @Inject
    CamelContext context;

    @Test
    public void testComponentBeansUnremovable() {
        Registry registry = context.getRegistry();

        Set<UnremovableComponentBean> unremovableComponentBeans = registry.findByType(UnremovableComponentBean.class);
        Assertions.assertEquals(1, unremovableComponentBeans.size());

        Set<UnremovableEndpointBean> unremovableEndpointBeans = registry.findByType(UnremovableEndpointBean.class);
        Assertions.assertEquals(1, unremovableEndpointBeans.size());
    }

    @Test
    public void testDataFormatBeansUnremovable() {
        Registry registry = context.getRegistry();
        Set<UnremovableDataFormatBean> unremovableDataFormatBeans = registry.findByType(UnremovableDataFormatBean.class);
        Assertions.assertEquals(1, unremovableDataFormatBeans.size());
    }

    @Test
    public void testLanguageBeansUnremovable() {
        Registry registry = context.getRegistry();
        Set<UnremovableLanguageBean> unremovableLanguageBeans = registry.findByType(UnremovableLanguageBean.class);
        Assertions.assertEquals(1, unremovableLanguageBeans.size());
    }

    @Test
    public void testInterceptStrategyUnremovable() {
        Registry registry = context.getRegistry();
        Set<UnremovableInterceptStrategy> unremovableInterceptStrategies = registry
                .findByType(UnremovableInterceptStrategy.class);
        Assertions.assertEquals(1, unremovableInterceptStrategies.size());
    }

    @Test
    public void testNonUnremovableBeansRemoved() {
        Registry registry = context.getRegistry();
        Set<Exception> nonUnremovableBeans = registry.findByType(Exception.class);
        Assertions.assertTrue(nonUnremovableBeans.isEmpty());
    }

    static final class UnremovableComponentBean {
    }

    static final class UnremovableEndpointBean {
    }

    static final class UnremovableDataFormatBean {
    }

    static final class UnremovableLanguageBean {
    }

    static final class UnremovableInterceptStrategy implements InterceptStrategy {
        @Override
        public Processor wrapProcessorInInterceptors(CamelContext context, NamedNode definition, Processor target,
                Processor nextTarget) throws Exception {
            return target;
        }
    }

    @ApplicationScoped
    static final class BeanProducers {

        @Singleton
        @Produces
        public UnremovableComponentBean unremovableComponentBean() {
            return new UnremovableComponentBean();
        }

        @Singleton
        @Produces
        public UnremovableEndpointBean unremovableEndpointBean() {
            return new UnremovableEndpointBean();
        }

        @Singleton
        @Produces
        public UnremovableDataFormatBean unremovableDataFormatBean() {
            return new UnremovableDataFormatBean();
        }

        @Singleton
        @Produces
        public UnremovableLanguageBean unremovableLanguageBean() {
            return new UnremovableLanguageBean();
        }

        @Singleton
        @Produces
        public UnremovableInterceptStrategy unremovableInterceptStrategy() {
            return new UnremovableInterceptStrategy();
        }

        @Singleton
        @Produces
        public Exception removableBean() {
            return new Exception("java.lang types should not be auto added as unremovable");
        }
    }

    // See component metadata in src/test/resources/custom-component.json
    @Component("custom-component")
    static final class CustomComponent {
    }

    // See dataformat metadata in src/test/resources/custom-dataformat.json
    @Dataformat("custom-dataformat")
    static final class CustomDataFormat {
    }

    // See language metadata in src/test/resources/custom-language.json
    @Language("custom-language")
    static final class CustomLanguage {
    }
}
