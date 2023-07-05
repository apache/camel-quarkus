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
package org.apache.camel.quarkus.component.atom.it;

import java.util.List;

import com.apptasticsoftware.rssreader.Item;
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
import org.apache.camel.Exchange;
import org.apache.camel.component.atom.AtomConstants;

@Path("/atom")
public class AtomResource {

    @Inject
    ConsumerTemplate consumerTemplate;

    @Path("/feed")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject consumeAtomFeed(@QueryParam("test-port") int port) throws Exception {
        Exchange exchange = consumerTemplate.receive("atom://http://localhost:" + port + "/atom.xml?splitEntries=false");
        List<Item> feed = exchange.getIn().getHeader(AtomConstants.ATOM_FEED, List.class);
        JsonArrayBuilder atom = Json.createArrayBuilder();

        for (Item entry : feed) {
            JsonObjectBuilder atomEntry = Json.createObjectBuilder();
            atomEntry.add("title", entry.getTitle().get());
            atomEntry.add("link", entry.getLink().get());
            atomEntry.add("comments", entry.getComments().get());
            atomEntry.add("description", entry.getDescription().get());
            atomEntry.add("author", entry.getAuthor().get());
            atom.add(atomEntry);
        }

        return Json.createObjectBuilder().add("entries", atom.build()).build();
    }
}
