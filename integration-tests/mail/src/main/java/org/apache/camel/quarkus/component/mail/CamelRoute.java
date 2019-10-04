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

import java.util.Properties;
import javax.enterprise.inject.Produces;
import javax.mail.Session;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mail.MailComponent;

public class CamelRoute extends RouteBuilder {
    @Override
    public void configure() {
        bindToRegistry("smtp", smtp());

        from("direct:mailtext")
            .setHeader("Subject", constant("Hello World"))
            .setHeader("To", constant("james@localhost"))
            .setHeader("From", constant("claus@localhost"))
            .to("smtp://localhost?initialDelay=100&delay=100");
    }

    @Produces
    MailComponent smtp() {
        MailComponent mail = new MailComponent(getContext());
        Session session = Session.getInstance(new Properties());
        mail.getConfiguration().setSession(session);
        return mail;
    }
}
