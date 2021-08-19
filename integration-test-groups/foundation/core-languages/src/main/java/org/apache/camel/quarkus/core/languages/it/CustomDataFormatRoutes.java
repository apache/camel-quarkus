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
package org.apache.camel.quarkus.core.languages.it;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spi.DataFormat;
import org.apache.camel.support.ExchangeHelper;

@ApplicationScoped
public class CustomDataFormatRoutes extends RouteBuilder {

    @Override
    public void configure() {
        from("direct:customDataFormatMarshal")
                .marshal().custom("customDataFormat");
        from("direct:customDataFormatUnmarshal")
                .unmarshal().custom("customDataFormat");
    }

    @Singleton
    @Named("customDataFormat")
    @Produces
    public DataFormat myExpression() {
        return new DataFormat() {

            @Override
            public void start() {
            }

            @Override
            public void stop() {
            }

            @Override
            public void marshal(Exchange exchange, Object graph, OutputStream stream) throws Exception {
                String payload = ExchangeHelper.convertToMandatoryType(exchange, String.class, graph);
                stream.write(("(" + payload + ")").getBytes(StandardCharsets.UTF_8));
                stream.flush();
            }

            @Override
            public Object unmarshal(Exchange exchange, InputStream stream) throws Exception {
                StringBuilder sb = new StringBuilder();
                try (InputStreamReader in = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    int c;
                    while ((c = in.read()) >= 0) {
                        sb.append((char) c);
                    }
                }
                if (sb.charAt(0) != '(' || sb.charAt(sb.length() - 1) != ')') {
                    throw new IllegalArgumentException("customDataFormat input must start with ( and end with )");
                }
                return sb.substring(1, sb.length() - 1);
            }
        };
    }

}
