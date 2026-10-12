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
package org.apache.camel.quarkus.test.extensions.annotation;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.quarkus.test.CamelQuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Continuous testing only detects composed {@code @QuarkusTest} annotations declared in the test sources, so
 * {@code @QuarkusTest} is needed next to {@code @CamelQuarkusTest}.
 */
@QuarkusTest
@CamelQuarkusTest(routes = AnnotationHelloET.Routes.class)
public class AnnotationHelloET {

    @Inject
    FluentProducerTemplate template;

    @Test
    public void helloSheldon() {
        Assertions.assertEquals("Hello Sheldon", hello());
    }

    @Test
    public void helloLeonard() {
        Assertions.assertEquals("Hello Leonard", hello());
    }

    @Test
    public void helloLeonardAgain() {
        Assertions.assertEquals("Hello Leonard", hello());
    }

    private String hello() {
        return template.to("direct:annotationHello").request(String.class);
    }

    public static class Routes extends RouteBuilder {
        @Override
        public void configure() {
            from("direct:annotationHello").setBody(simple("Hello {{camel-quarkus.junit.message}}"));
        }
    }
}
