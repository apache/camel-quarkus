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
package org.apache.camel.quarkus.component.elasticsearch.it;

import org.apache.camel.builder.RouteBuilder;

public class ElasticsearchRoutes extends RouteBuilder {

    @Override
    public void configure() {
        from("direct:bulk")
                .toD("${header.component}://elasticsearch?operation=Bulk");

        from("direct:delete")
                .toD("${header.component}://elasticsearch?operation=Delete");

        from("direct:deleteIndex")
                .toD("${header.component}://elasticsearch?operation=DeleteIndex");

        from("direct:exists")
                .toD("${header.component}://elasticsearch?operation=Exists");

        from("direct:get")
                .toD("${header.component}://elasticsearch?operation=GetById");

        from("direct:index")
                .toD("${header.component}://elasticsearch?operation=Index");

        from("direct:multiGet")
                .toD("${header.component}://elasticsearch?operation=MultiGet");

        from("direct:multiSearch")
                .toD("${header.component}://elasticsearch?operation=MultiSearch");

        from("direct:ping")
                .toD("${header.component}://elasticsearch?operation=Ping");

        from("direct:search")
                .toD("${header.component}://elasticsearch?operation=Search");

        from("direct:update")
                .toD("${header.component}://elasticsearch?operation=Update");
    }

}
