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
package org.apache.camel.quarkus.variables.it;

import org.apache.camel.builder.RouteBuilder;

public class VariablesRoutes extends RouteBuilder {

    public static String VARIABLE_NAME = "foo";
    public static String VARIABLE_VALUE = "bar";

    @Override
    public void configure() throws Exception {
        transformer().withDefaults();

        from("direct:setLocalVariableStart")
                .setVariable(VARIABLE_NAME)
                .constant(VARIABLE_VALUE)
                .to("mock:setLocalVariableEnd");

        from("direct:setGlobalVariableStart")
                .setVariable("global:" + VARIABLE_NAME)
                .constant(VARIABLE_VALUE)
                .to("mock:setGlobalVariableEnd");

        from("direct:removeLocalVariableStart")
                .setVariable(VARIABLE_NAME)
                .constant(VARIABLE_VALUE)
                .to("mock:removeLocalVariableMid")
                .removeVariable(VARIABLE_NAME)
                .to("mock:removeLocalVariableEnd");

        from("direct:removeGlobalVariableStart")
                .setVariable("global:" + VARIABLE_NAME)
                .constant(VARIABLE_VALUE)
                .to("mock:removeGlobalVariableMid")
                .removeVariable("global:" + VARIABLE_NAME);

        from("direct:setGlobalCustomStart")
                .id("customGlobalRepository")
                .autoStartup(false)
                .setVariable("global:" + VARIABLE_NAME)
                .constant(VARIABLE_VALUE)
                .to("mock:setGlobalCustomEnd");

        from("direct:convertStart")
                .setVariable(VARIABLE_NAME)
                .constant(11)
                .convertVariableTo(VARIABLE_NAME, Double.class)
                .to("mock:convertEnd");

        from("direct:filterStart")
                .setVariable("location", header("city"))
                .filter().method("my-bean", "matches")
                .to("mock:filterEnd");
    }
}
