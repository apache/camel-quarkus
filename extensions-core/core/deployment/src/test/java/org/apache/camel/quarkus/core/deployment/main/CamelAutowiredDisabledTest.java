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
package org.apache.camel.quarkus.core.deployment.main;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Properties;

import io.quarkus.test.QuarkusUnitTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.camel.Exchange;
import org.apache.camel.component.log.LogComponent;
import org.apache.camel.quarkus.main.CamelMain;
import org.apache.camel.spi.ExchangeFormatter;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class CamelAutowiredDisabledTest {
    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(applicationProperties(), "application.properties"));

    @Inject
    CamelMain main;

    public static Asset applicationProperties() {
        Writer writer = new StringWriter();

        Properties props = new Properties();
        props.setProperty("quarkus.banner.enabled", "false");
        props.setProperty("quarkus.arc.remove-unused-beans", "false");
        props.setProperty("camel.context.autowiredenabled", "false");

        try {
            props.store(writer, "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new StringAsset(writer.toString());
    }

    @Test
    public void testComponentAutoConfiguration() {
        // ensure that the exchange formatter explicit set to the LogComponent
        // is not overridden by any ExchangeFormatter instance available from
        // the container
        assertThat(main.getCamelContext().getComponent("myLog", LogComponent.class)).satisfies(component -> {
            assertThat(component.getExchangeFormatter()).isInstanceOf(MyExchangeFormatter.class);
        });

        // ensure that camel.context.autowiredenabled = false disables autowiring of the exchange formatter
        assertThat(main.getCamelContext().getComponent("log", LogComponent.class)).satisfies(component -> {
            assertThat(component.getExchangeFormatter()).isNull();
        });
    }

    @ApplicationScoped
    public static class BeanProducers {
        @Produces
        public ExchangeFormatter exchangeFormatter() {
            return new MyOtherExchangeFormatter();
        }

        @Named
        public LogComponent log() {
            return new LogComponent();
        }

        @Named
        public LogComponent myLog() {
            LogComponent component = new LogComponent();
            component.setExchangeFormatter(new MyExchangeFormatter());

            return component;
        }
    }

    public static class MyExchangeFormatter implements ExchangeFormatter {
        @Override
        public String format(Exchange exchange) {
            return exchange.toString();
        }
    }

    public static class MyOtherExchangeFormatter implements ExchangeFormatter {
        @Override
        public String format(Exchange exchange) {
            return exchange.toString();
        }
    }
}
