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
package org.apache.camel.quarkus.component.jacksonxml.deployment;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import io.quarkus.test.QuarkusUnitTest;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jacksonxml.JacksonXMLConstants;
import org.apache.camel.support.DefaultExchange;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class JacksonxmlTypeConverterTest {
    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(Routes.class)
                    .addClass(TestPojo.class));
    @Inject
    CamelContext context;

    @Test
    public void jacksonxmlConversion() throws Exception {
        Exchange exchange = new DefaultExchange(context);
        Map<String, String> body = new HashMap<>();
        Object convertedObject = context.getTypeConverter().convertTo(String.class, exchange, body);
        // will do a toString which is an empty map
        assertEquals(body.toString(), convertedObject);

        convertedObject = context.getTypeConverter().convertTo(Long.class, exchange,
                new HashMap<String, String>());
        assertNull(convertedObject);

        convertedObject = context.getTypeConverter().convertTo(long.class, exchange,
                new HashMap<String, String>());
        assertNull(convertedObject);
    }

    public static final class Routes extends RouteBuilder {
        @Override
        public void configure() {
            this.getContext().getGlobalOptions().put(JacksonXMLConstants.ENABLE_TYPE_CONVERTER, "true");
        }
    }

}
