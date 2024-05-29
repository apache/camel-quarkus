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
package org.apache.camel.quarkus.component.graphql.it;

import org.apache.camel.builder.RouteBuilder;

public class GraphQLRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        from("direct:addBookGraphQL")
                .toD("graphql://http://localhost:${header.port}/graphql?queryFile=graphql/addBookMutation.graphql&operationName=AddBook");

        from("direct:getBookGraphQL")
                .toD("graphql://http://localhost:${header.port}/graphql?queryFile=graphql/bookQuery.graphql&operationName=BookById");

        from("direct:getBookGraphQLAuthenticated")
                .toD("graphql://http://localhost:${header.port}/graphql?queryFile=graphql/bookQuery.graphql&operationName=BookById&username=camel&password=p4ssw0rd");

        from("direct:getQuery")
                .toD("graphql://http://localhost:${header.port}/graphql?query={books{id name}}");

        from("direct:addQueryVariables")
                .toD("graphql://http://localhost:${header.port}/graphql?queryFile=graphql/bookQuery.graphql&variables=#bookByIdQueryVariables");
    }
}
