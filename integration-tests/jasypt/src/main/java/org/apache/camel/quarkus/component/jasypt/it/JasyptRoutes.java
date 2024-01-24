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
package org.apache.camel.quarkus.component.jasypt.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class JasyptRoutes extends RouteBuilder {
    @ConfigProperty(name = "greeting.secret")
    String secretProperty;

    @ConfigProperty(name = "explicit.config.provider.secret")
    String secretExplicitConfigProviderProperty;

    @Override
    public void configure() throws Exception {
        from("direct:decryptConfiguration")
                .process("decryptConfig")
                .log("Decrypted: ${body}");

        from("direct:secretPropertyInjection")
                .setBody().constant(secretProperty)
                .log("Decrypted: ${body}");

        from("direct:secretExplicitConfigProviderPropertyInjection")
                .setBody().constant(secretExplicitConfigProviderProperty)
                .log("Decrypted: ${body}");

        from("timer:tick?delay={{timer.delay.secret}}&repeatCount={{timer.repeatCount.secret}}").id("secret-timer")
                .autoStartup(false)
                .setBody()
                .simple("delay = ${properties:timer.delay.secret}, repeatCount = ${properties:timer.repeatCount.secret}")
                .log("${body}")
                .to("mock:timerResult");
    }

    @Singleton
    @Named("decryptConfig")
    Processor decryptConfigProcessor() {
        return new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                Message message = exchange.getMessage();
                String configKey = message.getHeader("config.key", String.class);
                exchange.getContext()
                        .getPropertiesComponent()
                        .resolveProperty(configKey)
                        .ifPresent(message::setBody);
            }
        };
    }
}
