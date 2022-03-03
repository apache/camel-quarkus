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
package org.apache.camel.quarkus.component.mail;

import javax.enterprise.context.ApplicationScoped;

import org.apache.camel.builder.RouteBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class CamelRoute extends RouteBuilder {

    static final String EMAIL_ADDRESS = "test@localhost";
    static final String USERNAME = "test";
    static final String PASSWORD = "s3cr3t";

    @ConfigProperty(name = "mail.smtp.port")
    int smtpPort;

    @ConfigProperty(name = "mail.pop3.port")
    int pop3Port;

    @Override
    public void configure() {
        from("direct:sendMail")
                .toF("smtp://localhost:%d?username=%s&password=%s", smtpPort, USERNAME, PASSWORD);

        from("direct:sendMailWithAttachment")
                .toF("smtp://localhost:%d?username=%s&password=%s", smtpPort, USERNAME, PASSWORD);

        from("direct:mimeMultipartMarshal")
                .marshal().mimeMultipart();

        from("direct:mimeMultipartUnmarshalMarshal")
                .unmarshal().mimeMultipart()
                .marshal().mimeMultipart();

        fromF("pop3://localhost:%d?initialDelay=100&delay=500&username=%s&password=%s", pop3Port, USERNAME, PASSWORD)
                .to("seda:mail-pop3");
    }
}
