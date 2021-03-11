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
package org.apache.camel.quarkus.component.atlasmap.it;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.quarkus.component.atlasmap.it.model.Account;

public class Route extends RouteBuilder {
    @Override
    public void configure() throws Exception {

        // example of Routes that need the class Account to be registred for reflection
        from("platform-http:/atlasmap/json/java2csv?httpMethodRestrict=POST")
                .unmarshal().json(Account.class)
                .to("atlasmap:mapping/json/atlasmapping-java-to-csv.json");

        from("platform-http:/atlasmap/json/csv2java?httpMethodRestrict=POST")
                .to("atlasmap:mapping/json/atlasmapping-csv-to-java.json")
                .marshal().json(Account.class);
    }
}
