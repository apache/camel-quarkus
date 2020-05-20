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

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.mongodb.MongoGridFSException;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mongodb.gridfs.GridFsEndpoint;

@Path("/mongodb-gridfs")
public class MongodbGridfsResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/upload/{fileName}")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response uploadFile(@PathParam("fileName") String fileName, String content) throws Exception {
        Map<String, Object> headers = new HashMap<>();
        headers.put(Exchange.FILE_NAME, fileName);
        headers.put(Exchange.CONTENT_TYPE, "text/plain");

        Exchange result = producerTemplate.request("mongodb-gridfs:camelMongoClient?database=test&operation=create",
                exchange -> {
                    exchange.getMessage().setHeaders(headers);
                    exchange.getMessage().setBody(content);
                });

        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(result.getMessage().getHeader(GridFsEndpoint.GRIDFS_OBJECT_ID))
                .build();
    }

    @Path("/get/{fileName}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response retrieveFile(@PathParam("fileName") String fileName) {
        String result = null;
        try {
            result = producerTemplate.requestBodyAndHeader("mongodb-gridfs:camelMongoClient?database=test&operation=findOne",
                    null,
                    Exchange.FILE_NAME, fileName, String.class);
        } catch (Exception e) {
            if (e.getCause() instanceof MongoGridFSException) {
                return Response.status(404).build();
            }
        }
        return Response.ok().entity(result).build();
    }

    @Path("/delete/{fileName}")
    @DELETE
    public Response deleteFile(@PathParam("fileName") String fileName) {
        producerTemplate.requestBodyAndHeader("mongodb-gridfs:camelMongoClient?database=test&operation=remove", null,
                Exchange.FILE_NAME,
                fileName);
        return Response.noContent().build();
    }
}
