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
package org.apache.camel.quarkus.component.mongodb.it;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.camel.builder.RouteBuilder;
import org.bson.Document;

@ApplicationScoped
public class MongoDbRoute extends RouteBuilder {

    public static final String COLLECTION_TAILING = "tailingCollection";
    public static final String COLLECTION_PERSISTENT_TAILING = "persistentTailingCollection";
    public static final String COLLECTION_STREAM_CHANGES = "streamChangesCollection";

    @Inject
    @Named("results")
    Map<String, List<Document>> results;

    @Override
    public void configure() {
        from(String.format("mongodb:%s?database=test&collection=%s&tailTrackIncreasingField=increasing",
                MongoDbResource.DEFAULT_MONGO_CLIENT_NAME, COLLECTION_TAILING))
                        .process(e -> {
                            final List<Document> list = results.get(COLLECTION_TAILING);
                            synchronized (list) {
                                list.add(e.getMessage().getBody(Document.class));
                            }
                        });

        from(String.format(
                "mongodb:%s?database=test&collection=%s&tailTrackIncreasingField=increasing&persistentTailTracking=true&persistentId=darwin",
                MongoDbResource.DEFAULT_MONGO_CLIENT_NAME, COLLECTION_PERSISTENT_TAILING))
                        .id(COLLECTION_PERSISTENT_TAILING)
                        .process(e -> {
                            final List<Document> list = results.get(COLLECTION_PERSISTENT_TAILING);
                            synchronized (list) {
                                list.add(e.getMessage().getBody(Document.class));
                            }
                        });

        from(String.format("mongodb:%s?database=test&collection=%s&consumerType=changeStreams",
                MongoDbResource.DEFAULT_MONGO_CLIENT_NAME, COLLECTION_STREAM_CHANGES))
                        .routeProperty("streamFilter", "{'$match':{'$or':[{'fullDocument.string': 'value2'}]}}")
                        .process(e -> {
                            final List<Document> list = results.get(COLLECTION_STREAM_CHANGES);
                            synchronized (list) {
                                list.add(e.getMessage().getBody(Document.class));
                            }
                        });
    }

    @Produces
    @ApplicationScoped
    @Named("results")
    Map<String, List<Document>> results() {
        Map<String, List<Document>> result = new HashMap<>();
        result.put(COLLECTION_TAILING, new LinkedList<>());
        result.put(COLLECTION_PERSISTENT_TAILING, new LinkedList<>());
        result.put(COLLECTION_STREAM_CHANGES, new LinkedList<>());
        return result;
    }
}
