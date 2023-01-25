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
package org.apache.camel.quarkus.component.rss.it;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.ConsumerTemplate;

@Path("/rss")
public class RssResource {

    @Inject
    ConsumerTemplate consumerTemplate;

    @Path("/component")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject component(@QueryParam("test-port") int port) throws Exception {
        SyndFeed result = consumerTemplate.receiveBody("rss:http://localhost:" + port + "/feed.xml?splitEntries=false", 5000,
                SyndFeed.class);
        return processFeed(result);
    }

    @Path("/dataformat")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject dataformatMarshalUnmarshal() throws Exception {
        SyndFeed result = consumerTemplate.receiveBody("seda:end", 5000, SyndFeed.class);
        return processFeed(result);
    }

    private JsonObject processFeed(SyndFeed result) {
        JsonObjectBuilder channel = Json.createObjectBuilder();
        channel.add("title", result.getTitle());
        channel.add("description", result.getDescription());
        channel.add("link", result.getLink());

        JsonArrayBuilder items = Json.createArrayBuilder();

        for (SyndEntry entry : result.getEntries()) {
            JsonObjectBuilder atomEntry = Json.createObjectBuilder();
            atomEntry.add("title", entry.getTitle());
            atomEntry.add("link", entry.getLink());
            atomEntry.add("description", entry.getDescription().getValue());
            atomEntry.add("author", entry.getAuthor());
            items.add(atomEntry);
        }

        channel.add("items", items.build());

        return channel.build();
    }
}
