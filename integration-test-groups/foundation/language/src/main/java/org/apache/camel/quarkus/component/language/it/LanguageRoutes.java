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
package org.apache.camel.quarkus.component.language.it;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.util.Scanner;

@RegisterForReflection(targets = { Scanner.class })
public class LanguageRoutes extends RouteBuilder {

    @Inject
    CamelContext context;

    @Override
    public void configure() throws Exception {

        /* Language routes using scripts passed to endpoints */

        from("direct:languageBeanScript")
                .to("language://bean:org.apache.camel.quarkus.component.language.it.CaseConverter::toUpper");
        from("direct:languageConstantScript")
                .to("language://constant:Hello from constant language script");
        from("direct:languageExchangePropertyScript")
                .setProperty("testProperty", simple("Hello ${body} from exchangeProperty language script"))
                .to("language://exchangeProperty:testProperty");
        from("file:target?fileName=test-file.txt&noop=true")
                .to("language://file:File name is ${file:onlyname}")
                .to("direct:languageFileOutput");
        from("direct:languageHeaderScript")
                .setHeader("testHeader", simple("Hello ${body} from header language script"))
                .to("language://header:testHeader");
        from("direct:languageHl7terserScript")
                .to("language://hl7terser:PID-5")
                .setBody(simple("Patient's surname is ${body}"));
        from("direct:languageJsonPathScript")
                .to("language://jsonpath:$.message");
        from("direct:languageRefScript")
                .to("language://ref:lowerCase");
        from("direct:languageSimpleScript")
                .to("language://simple:Hello ${body} from simple language script");
        from("direct:languageTokenizeScript")
                .to("language://tokenize:,")
                .setBody(simple("${body.next()}"));
        from("direct:languageXpathScript")
                .to("language://xpath:/message/text()");
        from("direct:languageXqueryScript")
                .to("language://xquery:upper-case(/message/text())?resultType=String");

        /* Load simple scripts from resources */

        from("direct:languageSimpleResource")
                .to("language://simple:resource:hello.simple-res.txt");
        from("direct:languageSimpleFile")
                .to("language://simple:file:target/hello.simple-file.txt");

        /* Test transform option */

        from("direct:languageSimpleTransform")
                .to("language://simple:Hello ${body}!?transform=false");
    }
}
