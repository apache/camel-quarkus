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
import javax.inject.Inject;

import io.quarkus.test.QuarkusUnitTest;
import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.spi.Registry;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class CamelProducersTest {
    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Inject
    BeanUsingProducerTemplate usingProducerTemplate;
    @Inject
    BeanUsingProducerTemplate usingFluentProducerTemplate;
    @Inject
    BeanUsingConsumerTemplate usingConsumerTemplate;
    @Inject
    BeanUsingCamelContext usingCamelContext;
    @Inject
    BeanUsingRegistry usingRegistry;

    //@Test
    public void testInjection() throws Exception {
        usingProducerTemplate.verify();
        usingFluentProducerTemplate.verify();
        usingConsumerTemplate.verify();
        usingCamelContext.verify();
        usingRegistry.verify();
    }

    @ApplicationScoped
    static class BeanUsingProducerTemplate {
        @Inject
        ProducerTemplate target;
        @Inject
        @Produce("direct:start")
        ProducerTemplate targetWithUri;

        public void verify() throws Exception {
            assertThat(target).isNotNull();
            assertThat(target.getDefaultEndpoint()).isNull();
            assertThat(targetWithUri.getDefaultEndpoint().getEndpointUri()).isEqualTo("direct://start");
        }
    }

    @ApplicationScoped
    static class BeanUsingFluentProducerTemplate {
        @Inject
        FluentProducerTemplate target;
        @Inject
        @Produce("direct:start")
        FluentProducerTemplate targetWithUri;

        public void verify() throws Exception {
            assertThat(target).isNotNull();
            assertThat(target.getDefaultEndpoint()).isNull();
            assertThat(targetWithUri.getDefaultEndpoint().getEndpointUri()).isEqualTo("direct://start");
        }
    }

    @ApplicationScoped
    static class BeanUsingConsumerTemplate {
        @Inject
        ConsumerTemplate target;

        public void verify() throws Exception {
            assertThat(target).isNotNull();
        }
    }

    @ApplicationScoped
    static class BeanUsingCamelContext {
        @Inject
        CamelContext target;

        public void verify() throws Exception {
            assertThat(target).isNotNull();
            assertThat(target.getName()).startsWith("camel-");
        }
    }

    @ApplicationScoped
    static class BeanUsingRegistry {
        @Inject
        Registry target;

        public void verify() throws Exception {
            assertThat(target).isNotNull();
            assertThat(target.findByType(BeanUsingProducerTemplate.class)).hasSize(1);
            assertThat(target.findByType(BeanUsingConsumerTemplate.class)).hasSize(1);
        }
    }
}
