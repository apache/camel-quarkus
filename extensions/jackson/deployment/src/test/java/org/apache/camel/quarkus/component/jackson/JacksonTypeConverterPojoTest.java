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
package org.apache.camel.quarkus.component.jackson;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Properties;

import io.quarkus.test.QuarkusUnitTest;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonConstants;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JacksonTypeConverterPojoTest {
    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(Routes.class)
                    .addClass(Order.class)
                    .addAsResource(applicationProperties(), "application.properties"));
    private static ProducerTemplate producerTemplate;

    @Test
    public void jacksonConversionPojo() throws Exception {
        Order order = new Order();
        order.setAmount(1);
        order.setCustomerName("Acme");
        order.setPartName("Camel");

        String json = (String) producerTemplate.requestBody("direct:jackson-conversion-pojo", order);
        assertEquals("{\"id\":0,\"partName\":\"Camel\",\"amount\":1,\"customerName\":\"Acme\"}", json);

    }

    public static final class Routes extends RouteBuilder {
        @Override
        public void configure() {
            this.getContext().getGlobalOptions().put(JacksonConstants.ENABLE_TYPE_CONVERTER, "true");
            this.getContext().getGlobalOptions().put(JacksonConstants.TYPE_CONVERTER_TO_POJO, "true");
            from("direct:jackson-conversion-pojo").convertBodyTo(String.class);
            producerTemplate = this.getContext().createProducerTemplate();
        }
    }

    public static final Asset applicationProperties() {
        Writer writer = new StringWriter();

        Properties props = new Properties();
        props.setProperty("camel.context.name",
                "camel-quarkus-integration-tests-dataformats-json-jackson-type-converter-pojo");

        try {
            props.store(writer, "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new StringAsset(writer.toString());
    }
}
