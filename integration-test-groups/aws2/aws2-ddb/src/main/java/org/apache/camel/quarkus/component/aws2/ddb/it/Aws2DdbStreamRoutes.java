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
package org.apache.camel.quarkus.component.aws2.ddb.it;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.camel.builder.RouteBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.services.dynamodb.model.Record;
import software.amazon.awssdk.services.dynamodb.model.StreamRecord;

@ApplicationScoped
public class Aws2DdbStreamRoutes extends RouteBuilder {

    @ConfigProperty(name = "aws-ddb.stream-table-name")
    String streamTableName;

    @Inject
    @Named("aws2DdbStreamReceivedEvents")
    List<Map<String, String>> aws2DdbStreamReceivedEvents;

    @Override
    public void configure() throws Exception {
        from("aws2-ddbstream://" + streamTableName
                + "?streamIteratorType=FROM_LATEST")
                        .id("aws2DdbStreamRoute")
                        .autoStartup(false)
                        .process(e -> {
                            Record record = e.getMessage().getBody(Record.class);
                            StreamRecord item = record.dynamodb();
                            Map<String, String> result = new LinkedHashMap<>();
                            result.put("key", item.keys().get("key").s());
                            if (item.hasOldImage()) {
                                result.put("old", item.oldImage().get("value").s());
                            }
                            if (item.hasNewImage()) {
                                result.put("new", item.newImage().get("value").s());
                            }
                            result.put("sequenceNumber", item.sequenceNumber());
                            aws2DdbStreamReceivedEvents.add(result);
                        });
    }

    static class Producers {
        @Singleton
        @jakarta.enterprise.inject.Produces
        @Named("aws2DdbStreamReceivedEvents")
        List<Map<String, String>> aws2DdbStreamReceivedEvents() {
            return new CopyOnWriteArrayList<>();
        }
    }

}
