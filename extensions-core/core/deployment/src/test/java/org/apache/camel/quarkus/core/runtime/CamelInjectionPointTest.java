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

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.test.QuarkusUnitTest;
import org.apache.camel.Exchange;
import org.apache.camel.component.log.LogComponent;
import org.apache.camel.spi.ExchangeFormatter;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class CamelInjectionPointTest {
    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Inject
    Configurer configurer;
    @Inject
    Holder holder;

    //@Test
    public void testConfigurer() {
        assertThat(configurer.getLog()).isNotNull();
        assertThat(configurer.getLog().getExchangeFormatter()).isInstanceOf(MyExchangeFormatter.class);
        assertThat(holder.getLog()).isNotNull();
        assertThat(holder.getLog().getExchangeFormatter()).isInstanceOf(MyExchangeFormatter.class);
    }

    @ApplicationScoped
    public static class Configurer {
        @Inject
        LogComponent log;

        @PostConstruct
        void setUpLogComponent() {
            log.setExchangeFormatter(new MyExchangeFormatter());
        }

        public LogComponent getLog() {
            return log;
        }
    }

    @ApplicationScoped
    public static class Holder {
        private LogComponent log;

        @Inject
        public Holder(LogComponent log) {
            this.log = log;
        }

        public LogComponent getLog() {
            return log;
        }
    }

    public static class MyExchangeFormatter implements ExchangeFormatter {
        @Override
        public String format(Exchange exchange) {
            return exchange.toString();
        }
    }
}
