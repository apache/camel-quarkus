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
package org.apache.camel.quarkus.component.pinecone.it;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.pinecone.PineconeVectorDbHeaders;

import static org.apache.camel.component.pinecone.PineconeVectorDbAction.CREATE_SERVERLESS_INDEX;
import static org.apache.camel.component.pinecone.PineconeVectorDbAction.DELETE_INDEX;
import static org.apache.camel.component.pinecone.PineconeVectorDbAction.QUERY;
import static org.apache.camel.component.pinecone.PineconeVectorDbAction.UPSERT;

public class PineconeRoutes extends RouteBuilder {
    public static final String COLLECTION_NAME = "test-collection";
    public static final String INDEX_ID = "test-vectors";
    public static final String INDEX_NAME = "test-index";

    @Override
    public void configure() throws Exception {
        from("direct:createServerlessIndex")
                .setHeader(PineconeVectorDbHeaders.ACTION).constant(CREATE_SERVERLESS_INDEX)
                .setHeader(PineconeVectorDbHeaders.INDEX_NAME).constant(INDEX_NAME)
                .setHeader(PineconeVectorDbHeaders.COLLECTION_SIMILARITY_METRIC).constant("cosine")
                .setHeader(PineconeVectorDbHeaders.COLLECTION_DIMENSION).constant(3)
                .setHeader(PineconeVectorDbHeaders.COLLECTION_CLOUD).constant("aws")
                .setHeader(PineconeVectorDbHeaders.COLLECTION_CLOUD_REGION).constant("us-east-1")
                .toF("pinecone:%s", COLLECTION_NAME);

        from("direct:query")
                .setHeader(PineconeVectorDbHeaders.ACTION).constant(QUERY)
                .setHeader(PineconeVectorDbHeaders.INDEX_NAME).constant(INDEX_NAME)
                .setHeader(PineconeVectorDbHeaders.QUERY_TOP_K).constant(3)
                .toF("pinecone:%s", COLLECTION_NAME);

        from("direct:upsert")
                .setHeader(PineconeVectorDbHeaders.ACTION).constant(UPSERT)
                .setHeader(PineconeVectorDbHeaders.INDEX_ID).constant(INDEX_ID)
                .setHeader(PineconeVectorDbHeaders.INDEX_NAME).constant(INDEX_NAME)
                .toF("pinecone:%s", COLLECTION_NAME);

        from("direct:deleteIndex")
                .setHeader(PineconeVectorDbHeaders.ACTION).constant(DELETE_INDEX)
                .setHeader(PineconeVectorDbHeaders.INDEX_NAME).constant(INDEX_NAME)
                .toF("pinecone:%s", COLLECTION_NAME);
    }
}
