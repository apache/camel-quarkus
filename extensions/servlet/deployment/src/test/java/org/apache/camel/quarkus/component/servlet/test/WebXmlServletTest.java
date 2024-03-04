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

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import org.apache.camel.builder.RouteBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.hamcrest.Matchers.equalTo;

public class WebXmlServletTest {
    static final String WEB_XML = """
            <web-app>
                <servlet>
                  <servlet-name>my-servlet</servlet-name>
                  <servlet-class>org.apache.camel.component.servlet.CamelHttpTransportServlet</servlet-class>
                  <load-on-startup>1</load-on-startup>
                </servlet>

                <servlet-mapping>
                  <servlet-name>my-servlet</servlet-name>
                  <url-pattern>/*</url-pattern>
                </servlet-mapping>
            </web-app>
            """;
    static final String MESSAGE = "This servlet was configured from web.xml";

    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(new StringAsset(WEB_XML), "META-INF/web.xml"));

    @Test
    public void noDefaultServlet() throws Exception {
        RestAssured.when().get("/web/xml").then()
                .body(equalTo(MESSAGE));
    }

    public static final class Routes extends RouteBuilder {
        @Override
        public void configure() {
            from("servlet://web/xml?servletName=my-servlet")
                    .setBody(constant(MESSAGE));
        }
    }
}
