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
package org.apache.camel.quarkus.test.common;

import io.quarkus.test.junit.QuarkusTest;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.junit.jupiter.api.Test;

@QuarkusTest
public abstract class AbstractSimpleMockTest extends CamelQuarkusTestSupport {

    @Produce("direct:start")
    protected ProducerTemplate template;

    private String msgToSend;
    private String msgToExpect;

    public AbstractSimpleMockTest(String msgToSend, String msgToExpect) {
        this.msgToSend = msgToSend;
        this.msgToExpect = msgToExpect;
    }

    public void setMsgToSend(String msgToSend) {
        this.msgToSend = msgToSend;
    }

    @Test
    public void testMock() throws Exception {
        getMockEndpoint("mock:result").expectedBodiesReceived(msgToExpect);

        template.sendBody("direct:start", msgToSend);

        MockEndpoint.assertIsSatisfied(context);
    }

    @Override
    protected RoutesBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:start").to("mock:result");
            }
        };
    }

}
