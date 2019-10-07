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
package org.acme.rest.json;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;

public class Routes extends RouteBuilder {
    private final Set<Fruit> fruits;
    private final Set<Legume> legumes;

    public Routes() {
        this.fruits =  Collections.newSetFromMap(Collections.synchronizedMap(new LinkedHashMap<>()));
        this.fruits.add(new Fruit("Apple", "Winter fruit"));
        this.fruits.add(new Fruit("Pineapple", "Tropical fruit"));

        this.legumes = Collections.synchronizedSet(new LinkedHashSet<>());
        this.legumes.add(new Legume("Carrot", "Root vegetable, usually orange"));
        this.legumes.add(new Legume("Zucchini", "Summer squash"));
    }

    @Override
    public void configure() throws Exception {
        from("platform-http:/legumes?httpMethodRestrict=GET")
            .setBody().constant(legumes)
            .marshal().json();

        from("platform-http:/fruits?httpMethodRestrict=GET,POST")
            .choice()
                .when(simple("${header.CamelHttpMethod} == 'GET'"))
                    .setBody()
                        .constant(fruits)
                    .endChoice()
                .when(simple("${header.CamelHttpMethod} == 'POST'"))
                    .unmarshal()
                        .json(JsonLibrary.Jackson, Fruit.class)
                    .process()
                        .body(Fruit.class, fruits::add)
                    .setBody()
                        .constant(fruits)
                    .endChoice()
            .end()
            .marshal().json();
    }
}
