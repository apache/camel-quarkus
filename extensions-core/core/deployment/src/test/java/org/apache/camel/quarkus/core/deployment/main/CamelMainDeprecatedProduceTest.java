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

import io.quarkus.test.QuarkusUnitTest;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.Produce;
import org.apache.camel.builder.RouteBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class CamelMainDeprecatedProduceTest {
    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest().setExpectedException(IllegalArgumentException.class)
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Produce(uri = "direct:start")
    FluentProducerTemplate produceProducerFluent;

    @Test
    public void produceAnnotationWithDeprecatedParamsThrowsIllegalArgumentException() {
        // Noop - we expect IllegalArgumentException to be thrown on application startup
    }

    public static class MyRoutes extends RouteBuilder {

        @Override
        public void configure() throws Exception {
            from("direct:start").to("direct:end");
        }
    }
}
