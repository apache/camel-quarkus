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
package org.apache.camel.quarkus.support.language.deployment;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Properties;
import java.util.function.Consumer;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PropertiesToSetTest {

    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .addBuildChainCustomizer(new Consumer<>() {
                @Override
                public void accept(BuildChainBuilder buildChainBuilder) {
                    buildChainBuilder.addFinal(ExpressionExtractionResultBuildItem.class);
                }
            })
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(applicationProperties(), "application.properties"));

    public static Asset applicationProperties() {
        Writer writer = new StringWriter();

        Properties props = new Properties();
        props.setProperty("quarkus.camel.expression.on-build-time-analysis-failure", "fail");

        try {
            props.store(writer, "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new StringAsset(writer.toString());
    }

    @Inject
    CamelContext context;

    @Test
    void routeWithPropertiesToSet() {
        assertEquals(1, context.getRoutesSize());
    }

    public static class Routes extends RouteBuilder {
        @Override
        public void configure() throws Exception {
            from("timer:myTimer?period=1&repeatCount=1&delay=1")
                    .log("body value is: ${body}");
        }
    }
}
