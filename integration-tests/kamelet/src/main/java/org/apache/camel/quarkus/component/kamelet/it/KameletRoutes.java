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
package org.apache.camel.quarkus.component.kamelet.it;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;

@ApplicationScoped
public class KameletRoutes extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        routeTemplate("setBody")
                .templateParameter("bodyValue")
                .from("kamelet:source")
                .setBody().constant("Hello {{bodyValue}}");

        routeTemplate("setBodyFromProperties")
                .templateParameter("bodyValueFromProperty")
                .from("kamelet:source")
                .setBody().constant("Hello {{bodyValueFromProperty}}");

        routeTemplate("tick")
                .from("timer:{{routeId}}?repeatCount=1&delay=-1")
                .setBody().exchangeProperty(Exchange.TIMER_COUNTER)
                .to("kamelet:sink");

        routeTemplate("echo")
                .templateParameter("prefix")
                .templateParameter("suffix")
                .from("kamelet:source")
                .setBody().simple("{{prefix}} ${body} {{suffix}}");

        routeTemplate("AppendWithBean")
                .templateBean("appender", new AppenderProcessor())
                .from("kamelet:source")
                .to("bean:{{appender}}");

        routeTemplate("AppendWithClass")
                .templateBean("appender", AppenderProcessor.class)
                .from("kamelet:source")
                .to("bean:{{appender}}");

        from("direct:chain")
                .to("kamelet:echo/1?prefix=Camel Quarkus&suffix=Chained")
                .to("kamelet:echo/2?prefix=Hello&suffix=Route");

        from("direct:kamelet-location-at-runtime")
                .kamelet("upper?location=classpath:kamelets-runtime/upper-kamelet.xml");
    }

    @RegisterForReflection
    public static class AppenderProcessor implements Processor {
        @Override
        public void process(Exchange exchange) {
            exchange.getMessage().setBody(exchange.getMessage().getBody(String.class) + "-suffix");
        }
    }

    @RegisterForReflection(fields = false, targets = { String.class })
    public static class StringUpperCaseReflectionForUpperKamelet {
    }
}
