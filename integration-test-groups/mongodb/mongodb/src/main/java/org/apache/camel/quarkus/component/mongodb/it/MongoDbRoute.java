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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.quarkus.test.support.mongodb.MongoDbConstants;
import org.bson.Document;

@ApplicationScoped
public class MongoDbRoute extends RouteBuilder {

    @Inject
    @Named("results")
    Map<String, List<Document>> results;

    @Override
    public void configure() {
        fromF("mongodb:%s?database=test&collection=%s&tailTrackIncreasingField=increasing",
                MongoDbConstants.DEFAULT_MONGO_CLIENT_NAME, MongoDbConstants.COLLECTION_TAILING)
                        .process(e -> {
                            final List<Document> list = results.get(MongoDbConstants.COLLECTION_TAILING);
                            synchronized (list) {
                                list.add(e.getMessage().getBody(Document.class));
                            }
                        });

        fromF("mongodb:%s?database=test&collection=%s&tailTrackIncreasingField=increasing&persistentTailTracking=true&persistentId=darwin",
                MongoDbConstants.DEFAULT_MONGO_CLIENT_NAME, MongoDbConstants.COLLECTION_PERSISTENT_TAILING)
                        .id(MongoDbConstants.COLLECTION_PERSISTENT_TAILING)
                        .process(e -> {
                            final List<Document> list = results.get(MongoDbConstants.COLLECTION_PERSISTENT_TAILING);
                            synchronized (list) {
                                list.add(e.getMessage().getBody(Document.class));
                            }
                        });

        fromF("mongodb:%s?database=test&collection=%s&consumerType=changeStreams&streamFilter=%s",
                MongoDbConstants.DEFAULT_MONGO_CLIENT_NAME, MongoDbConstants.COLLECTION_STREAM_CHANGES,
                "{'$match':{'$or':[{'fullDocument.string': 'value2'}]}}")
                        .process(e -> {
                            final List<Document> list = results.get(MongoDbConstants.COLLECTION_STREAM_CHANGES);
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
        result.put(MongoDbConstants.COLLECTION_TAILING, new LinkedList<>());
        result.put(MongoDbConstants.COLLECTION_PERSISTENT_TAILING, new LinkedList<>());
        result.put(MongoDbConstants.COLLECTION_STREAM_CHANGES, new LinkedList<>());
        return result;
    }
}
