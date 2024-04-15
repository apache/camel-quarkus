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
package org.apache.camel.quarkus.component.jt400.it;

import com.ibm.as400.access.AS400Message;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jt400.Jt400Constants;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class Jt400Routes extends RouteBuilder {
    private static final Logger LOGGER = Logger.getLogger(Jt400Routes.class);

    @ConfigProperty(name = "cq.jt400.library")
    String jt400Library;

    @ConfigProperty(name = "cq.jt400.url")
    String jt400Url;

    @ConfigProperty(name = "cq.jt400.username")
    String jt400Username;

    @ConfigProperty(name = "cq.jt400.password")
    String jt400Password;

    @ConfigProperty(name = "cq.jt400.message-replyto-queue")
    String jt400MessageReplyToQueue;

    @Inject
    InquiryMessageHolder inquiryMessageHolder;

    @Override
    public void configure() throws Exception {
        from(getUrlForLibrary(jt400MessageReplyToQueue + "?sendingReply=true"))
                .id("inquiryRoute")
                //route has tobe stopped to avoid "CPF2451 Message queue REPLYMSGQ is allocated to another job."
                .autoStartup(false)
                .choice()
                .when(header(Jt400Constants.MESSAGE_TYPE).isEqualTo(AS400Message.INQUIRY))
                .process((exchange) -> {
                    String msg = exchange.getIn().getBody(String.class);
                    LOGGER.debug(
                            "Inquiry route: received '" + msg + "' (expecting  '" + inquiryMessageHolder.getMessageText()
                                    + "')");
                    if (inquiryMessageHolder.getMessageText() != null && !inquiryMessageHolder.getMessageText().equals(msg)) {
                        throw new IllegalStateException(
                                "Intentional! Current exchange is not triggered by current test process, therefore ignoring the exchange");
                    }
                    String reply = "reply to: " + msg;
                    exchange.getIn().setBody(reply);
                })
                .to(getUrlForLibrary(jt400MessageReplyToQueue))
                .process(e -> inquiryMessageHolder.setProcessed(true));
    }

    private String getUrlForLibrary(String suffix) {
        return String.format("jt400://%s:%s@%s%s", jt400Username, jt400Password, jt400Url,
                "/QSYS.LIB/" + jt400Library + ".LIB/" + suffix);
    }
}
