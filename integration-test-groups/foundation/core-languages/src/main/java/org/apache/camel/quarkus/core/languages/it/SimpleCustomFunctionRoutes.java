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
package org.apache.camel.quarkus.core.languages.it;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spi.SimpleFunction;
import org.apache.camel.spi.SimpleFunctionRegistry;
import org.apache.camel.support.ExpressionAdapter;
import org.apache.camel.support.PluginHelper;

public class SimpleCustomFunctionRoutes extends RouteBuilder {

    @Override
    public void configure() {
        SimpleFunctionRegistry registry = PluginHelper.getSimpleFunctionRegistry(getContext());

        registry.addFunction(new SimpleFunction() {
            @Override
            public String getName() {
                return "greet";
            }

            @Override
            public Object apply(Exchange exchange, Object input) {
                return "Hello " + input;
            }
        });

        registry.addFunction("shout", new ExpressionAdapter() {
            @Override
            public Object evaluate(Exchange exchange) {
                return exchange.getMessage().getBody(String.class).toUpperCase() + "!";
            }
        });

        from("direct:simpleCustomFunctionBean")
                .transform().simple("${function(reverseText)}");

        from("direct:simpleCustomFunctionBeanShorthand")
                .transform().simple("${reverseText(${header.text})}");

        from("direct:simpleCustomFunction")
                .transform().simple("${function(greet)}");

        from("direct:simpleCustomFunctionWithParameter")
                .transform().simple("${function(greet,${header.name})}");

        from("direct:simpleCustomFunctionExpression")
                .transform().simple("${shout()}");
    }
}
