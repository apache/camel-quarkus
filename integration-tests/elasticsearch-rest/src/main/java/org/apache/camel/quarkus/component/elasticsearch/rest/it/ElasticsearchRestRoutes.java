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
package org.apache.camel.quarkus.component.elasticsearch.rest.it;

import javax.inject.Named;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.elasticsearch.ElasticsearchComponent;
import org.elasticsearch.client.RestClient;

public class ElasticsearchRestRoutes extends RouteBuilder {

    @Override
    public void configure() {
        from("direct:bulk")
                .toD("${header.component}://elasticsearch?operation=Bulk");

        from("direct:bulkIndex")
                .toD("${header.component}://elasticsearch?operation=BulkIndex");

        from("direct:delete")
                .toD("${header.component}://elasticsearch?operation=Delete");

        from("direct:deleteIndex")
                .to("elasticsearch-rest://elasticsearch?operation=DeleteIndex");

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

    @Named("elasticsearch-rest-quarkus")
    public ElasticsearchComponent elasticsearchQuarkus(RestClient client) {
        // Use the RestClient bean created by the Quarkus ElasticSearch extension
        ElasticsearchComponent component = new ElasticsearchComponent();
        component.setClient(client);
        return component;
    }
}
