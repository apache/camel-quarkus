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
package org.apache.camel.quarkus.test.junit5.patterns;

import java.util.Properties;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.apache.camel.Endpoint;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(AdviceWithAnotherRouteTest.class)
public class AdviceWithAnotherRouteTest extends CamelQuarkusTestSupport {

    @Override
    public boolean isUseAdviceWith() {
        return true;
    }

    @BeforeEach
    public void doSomethingBefore() throws Exception {
        AdviceWithRouteBuilder mocker = new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:sftp");

                interceptSendToEndpoint("file:*").skipSendToOriginalEndpoint().to("mock:file");
            }
        };
        AdviceWith.adviceWith(this.context.adapt(ModelCamelContext.class).getRouteDefinition("myRoute"), this.context, mocker);

        startRouteDefinitions();
    }

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        Properties pc = new Properties();
        pc.put("ftp.username", "scott");
        pc.put("ftp.password", "tiger");
        return pc;
    }

    @Test
    public void testOverride() throws Exception {

        getMockEndpoint("mock:file").expectedMessageCount(1);
        getMockEndpoint("mock:result").expectedMessageCount(1);
        template.sendBody("direct:start", "Haha");
        template.sendBody("direct:sftp", "Hello World");
        Endpoint endpoint = getMockEndpoint("mock:start", true);
        System.out.println(endpoint);

        MockEndpoint.assertIsSatisfied(context);
    }

    @Override
    protected RouteBuilder[] createRouteBuilders() {
        return new RouteBuilder[] { new RouteBuilder() {
            public void configure() {
                from("direct:start").to("mock:result");
            }
        }, new RouteBuilder() {
            public void configure() {
                from("ftp:somepath?username={{ftp.username}}&password={{ftp.password}}").routeId("myRoute")
                        .log("{{ftp.username}} is hahah")
                        .to("file:target/out");
            }
        } };
    }
}
