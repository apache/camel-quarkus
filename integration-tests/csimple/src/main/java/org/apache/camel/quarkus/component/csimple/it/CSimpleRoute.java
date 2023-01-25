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
package org.apache.camel.quarkus.component.csimple.it;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.apache.camel.builder.RouteBuilder;

@RegisterForReflection(targets = String.class)
public class CSimpleRoute extends RouteBuilder {

    @Override
    public void configure() {
        routeTemplate("whereTo")
                .templateParameter("bar")
                .templateBean("myBar", String.class, "csimple", "Hi")
                .from("kamelet:source")
                .to("bean:{{myBar}}?method=toString")
                .setBody().csimple("${body} {{bar}}");
        from("direct:csimple-hi")
                .kamelet("whereTo?bar=Bill");
        from("direct:csimple-hello")
                .setBody().csimple("Hello ${body}");
        from("direct:predicate")
                .choice()
                .when().csimple("${body} > 10")
                .setBody().constant("High").endChoice()
                .otherwise()
                .setBody().constant("Low").endChoice();
    }

}
