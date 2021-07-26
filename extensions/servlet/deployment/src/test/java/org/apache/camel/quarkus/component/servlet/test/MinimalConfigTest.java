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
package org.apache.camel.quarkus.component.servlet.test;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Properties;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import org.apache.camel.builder.RouteBuilder;
import org.hamcrest.core.IsEqual;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.RegisterExtension;

public class MinimalConfigTest {
    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(CustomDefaultServletClassTest.Routes.class)
                    .addAsResource(applicationProperties(), "application.properties"));

    //@Test
    public void minimal() throws Exception {
        RestAssured.when().get("/hello")
                .then().body(IsEqual.equalTo("GET: /hello"));
    }

    public static final class Routes extends RouteBuilder {
        @Override
        public void configure() {
            from("servlet://hello?matchOnUriPrefix=true")
                    .setBody(constant("GET: /hello"));
        }
    }

    public static final Asset applicationProperties() {
        Writer writer = new StringWriter();

        Properties props = new Properties();
        props.setProperty("quarkus.camel.servlet.url-patterns", "/*");

        try {
            props.store(writer, "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new StringAsset(writer.toString());
    }
}
