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
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.quarkus.runtime.StartupEvent;
import org.apache.camel.ConsumerTemplate;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.services.dynamodb.model.Record;
import software.amazon.awssdk.services.dynamodb.model.StreamRecord;

@Path("/aws2-ddbstream")
@ApplicationScoped
public class Aws2DdbStreamResource {

    @ConfigProperty(name = "aws-ddb.table-name")
    String tableName;

    @Inject
    ConsumerTemplate consumerTemplate;

    void startup(@Observes StartupEvent event) {
        /* Hit the consumer URI at application startup so that the consumer starts polling eagerly */
        consumerTemplate.receiveBody(componentUri(), 1000);
    }

    @Path("/change")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> change() {
        Map<String, String> result = new LinkedHashMap<>();
        Record record = consumerTemplate.receiveBody(componentUri(), 10000, Record.class);
        if (record == null) {
            return null;
        }
        StreamRecord item = record.dynamodb();
        result.put("key", item.keys().get("key").s());
        if (item.hasOldImage()) {
            result.put("old", item.oldImage().get("value").s());
        }
        if (item.hasNewImage()) {
            result.put("new", item.newImage().get("value").s());
        }
        return result;
    }

    private String componentUri() {
        return "aws2-ddbstream://" + tableName;
    }

}
