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
package org.apache.camel.quarkus.component.json.path.it;

import com.jayway.jsonpath.Option;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.jsonpath.JsonPath;

import io.quarkus.runtime.annotations.RegisterForReflection;

public class JsonPathTestRoute extends RouteBuilder {

    @Override
    public void configure() {
        from("direct:getBookPriceLevel")
                .choice()
                .when().jsonpath("$.store.book[?(@.price < 10)]")
                .setBody(constant("cheap"))
                .when().jsonpath("$.store.book[?(@.price < 30)]")
                .setBody(constant("average"))
                .otherwise()
                .setBody(constant("expensive"));

        from("direct:getBookPrice").setBody().jsonpath("$.store.book.price");

        from("direct:getFullName").bean(FullNameBean.class);

        from("direct:getAllCarColors").transform().jsonpath("$.cars[*].color");
    }

    @RegisterForReflection
    protected static class FullNameBean {
        // middle name is optional
        public static String getName(@JsonPath("person.firstName") String first,
                @JsonPath(value = "person.middleName", options = Option.SUPPRESS_EXCEPTIONS) String middle,
                @JsonPath("person.lastName") String last) {
            if (middle != null) {
                return first + " " + middle + " " + last;
            }
            return first + " " + last;
        }
    }
}
