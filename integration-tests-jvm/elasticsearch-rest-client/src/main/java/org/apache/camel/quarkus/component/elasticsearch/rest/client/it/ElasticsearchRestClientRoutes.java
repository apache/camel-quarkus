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
import org.apache.camel.component.elasticsearch.rest.client.ElasticsearchRestClientOperation;

import static org.apache.camel.component.elasticsearch.rest.client.ElasticsearchRestClientOperation.*;

public class ElasticsearchRestClientRoutes extends RouteBuilder {
    @Override
    public void configure() {
        from("direct:createIndex")
                .to(elasticsearchRestClient(CREATE_INDEX));

        from("direct:createIndexSettings")
                .to(elasticsearchRestClient(CREATE_INDEX));

        from("direct:delete")
                .to(elasticsearchRestClient(DELETE));

        from("direct:deleteIndex")
                .to(elasticsearchRestClient(DELETE_INDEX));

        from("direct:get")
                .to(elasticsearchRestClient(GET_BY_ID))
                .convertBodyTo(String.class)
                .unmarshal().json(Document.class);

        from("direct:index")
                .marshal().json()
                .to(elasticsearchRestClient(INDEX_OR_UPDATE));

        from("direct:search")
                .to(elasticsearchRestClient(SEARCH));

        from("direct:searchQuery")
                .to(elasticsearchRestClient(SEARCH));
    }

    private String elasticsearchRestClient(ElasticsearchRestClientOperation operation) {
        return "elasticsearch-rest-client:camel-quarkus" +
                "?operation=" + operation +
                "&hostAddressesList={{camel.elasticsearch-rest-client.host-addresses-list}}" +
                "&user={{camel.elasticsearch-rest-client.user}}" +
                "&password={{camel.elasticsearch-rest-client.password}}" +
                "&certificatePath={{camel.elasticsearch-rest-client.cert}}" +
                "&enableSniffer=true";
    }

}
