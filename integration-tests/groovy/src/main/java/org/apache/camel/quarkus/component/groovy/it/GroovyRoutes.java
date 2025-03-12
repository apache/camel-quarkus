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
package org.apache.camel.quarkus.component.groovy.it;

import java.util.Optional;

import jakarta.activation.DataHandler;
import jakarta.activation.FileDataSource;
import org.apache.camel.attachment.AttachmentMessage;
import org.apache.camel.builder.RouteBuilder;
import org.eclipse.microprofile.config.ConfigProvider;

public class GroovyRoutes extends RouteBuilder {

    @Override
    public void configure() {

        Optional<Boolean> inNative = ConfigProvider.getConfig().getOptionalValue("quarkus.native.enabled", Boolean.class);

        routeTemplate("whereTo")
                .templateParameter("bar")
                .templateBean("myBar", "groovy", "resource:classpath:bean.txt")
                .from("kamelet:source")
                .to("bean:{{myBar}}");

        from("direct:groovyHi")
                .kamelet("whereTo?bar=Shamrock");

        from("direct:groovyHello")
                .transform().groovy("\"Hello \" + body + \" from Groovy!\"");

        from("direct:filter")
                .filter().groovy("!(request.body as String).contains('Hi')")
                .transform().groovy("\"Received unknown request: \" + body");

        from("direct:predicate")
                .choice()
                .when().groovy("((int) body) / 2 > 10")
                .setBody().constant("High").endChoice()
                .otherwise()
                .setBody().constant("Low").endChoice();

        from("direct:scriptGroovy")
                .script()
                .groovy("exchange.getMessage().setBody('Hello ' + exchange.getMessage().getBody(String.class) + ' from Groovy!')");

        from("direct:multiStatement")
                .transform().groovy("""
                        def a = "Hello A"
                        def b = "Hello B"
                        //other statements
                        def result = "Hello C"
                        """);

        from("direct:scriptFromResource")
                .setHeader("myHeader").groovy("resource:classpath:mygroovy.groovy")
                .setBody(simple("${header.myHeader}"));

        from("direct:validateContext")
                .setHeader("myHeader", constant("myHeaderValue"))
                .setVariable("myVariable", constant("myVariableValue"))
                .setProperty("myProperty", constant("myPropertyValue"))
                .process(exchange -> {
                    AttachmentMessage attMsg = exchange.getIn(AttachmentMessage.class);
                    attMsg.addAttachment("mygroovy.groovy", new DataHandler(new FileDataSource("mygroovy.groovy")));
                })
                .transform().groovy("return " + getContextVariables(false))
                .log("${body}");

        //routes only for jvm
        if (inNative.isEmpty() || !inNative.get()) {
            from("direct:customizedHi")
                    .transform().groovy("hello + \" \" + body + \" from Groovy!\"");

            from("direct:validateContextInJvm")
                    .setHeader("myHeader", constant("myHeaderValue"))
                    .setVariable("myVariable", constant("myVariableValue"))
                    .setProperty("myProperty", constant("myPropertyValue"))
                    .process(exchange -> {
                        AttachmentMessage attMsg = exchange.getIn(AttachmentMessage.class);
                        attMsg.addAttachment("mygroovy.groovy", new DataHandler(new FileDataSource("mygroovy.groovy")));
                    })
                    .transform().groovy("return " + getContextVariables(true))
                    .log("${body}");
        }
    }

    private static String getContextVariables(boolean jvm) {
        String contextVariables = "\"headers: \" + headers + " +
                "\", exchange: \" + exchange + " +
                "\", camelContext: \" + camelContext + " +
                "\", request: \" + request";

        if (jvm) {
            // following properties are not working in the native mode
            contextVariables += " + \", exchangeProperties: \" + exchangeProperties + " +
                    "\", exchangeProperty: \" + exchangeProperty + " +
                    "\", variable: \" + variable + " +
                    "\", variables: \" + variables + " +
                    "\", header: \" + header + " +
                    "\", attachments: \" + attachments + " +
                    "\", log: \" + log";
        }
        return contextVariables;
    }
}
