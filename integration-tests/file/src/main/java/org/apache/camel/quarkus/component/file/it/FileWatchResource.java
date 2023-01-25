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
package org.apache.camel.quarkus.component.file.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.component.file.watch.FileWatchConstants;
import org.apache.camel.component.file.watch.constants.FileEventEnum;

@Path("/file-watch")
@ApplicationScoped
public class FileWatchResource {

    @Inject
    ConsumerTemplate consumerTemplate;

    @Path("/get-events")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEvent(@QueryParam("path") String path) throws Exception {
        final Exchange exchange = consumerTemplate.receiveNoWait("file-watch://" + path);
        if (exchange == null) {
            return Response.noContent().build();
        } else {
            final Message message = exchange.getMessage();
            final ObjectMapper mapper = new ObjectMapper();
            final ObjectNode node = mapper.createObjectNode();
            node.put("type", message.getHeader(FileWatchConstants.EVENT_TYPE_HEADER, FileEventEnum.class).toString());
            node.put("path", message.getHeader("CamelFileAbsolutePath", String.class));
            return Response
                    .ok()
                    .entity(node)
                    .build();
        }
    }

}
