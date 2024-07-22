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
package org.apache.camel.quarkus.component.elasticsearch.rest.client.it;

import org.apache.camel.builder.RouteBuilder;

import static org.apache.camel.component.elasticsearch.rest.client.ElasticsearchRestClientOperation.CREATE_INDEX;
import static org.apache.camel.component.elasticsearch.rest.client.ElasticsearchRestClientOperation.DELETE;
import static org.apache.camel.component.elasticsearch.rest.client.ElasticsearchRestClientOperation.DELETE_INDEX;
import static org.apache.camel.component.elasticsearch.rest.client.ElasticsearchRestClientOperation.GET_BY_ID;
import static org.apache.camel.component.elasticsearch.rest.client.ElasticsearchRestClientOperation.INDEX_OR_UPDATE;
import static org.apache.camel.component.elasticsearch.rest.client.ElasticsearchRestClientOperation.SEARCH;

public class ElasticsearchRestClientRoutes extends RouteBuilder {
    @Override
    public void configure() {
        from("direct:createIndex")
                .toF("elasticsearch-rest-client:camel-quarkus?operation=%s", CREATE_INDEX);

        from("direct:createIndexSettings")
                .toF("elasticsearch-rest-client:camel-quarkus?operation=%s", CREATE_INDEX);

        from("direct:delete")
                .toF("elasticsearch-rest-client:camel-quarkus?operation=%s", DELETE);

        from("direct:deleteIndex")
                .toF("elasticsearch-rest-client:camel-quarkus?operation=%s", DELETE_INDEX);

        from("direct:get")
                .toF("elasticsearch-rest-client:camel-quarkus?operation=%s", GET_BY_ID)
                .convertBodyTo(String.class)
                .unmarshal().json(Document.class);

        from("direct:index")
                .marshal().json()
                .toF("elasticsearch-rest-client:camel-quarkus?operation=%s", INDEX_OR_UPDATE);

        from("direct:search")
                .toF("elasticsearch-rest-client:camel-quarkus?operation=%s", SEARCH);

        from("direct:searchQuery")
                .toF("elasticsearch-rest-client:camel-quarkus?operation=%s", SEARCH);
    }
}
