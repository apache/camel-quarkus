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
package org.apache.camel.quarkus.component.mongodb.gridfs.it;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.mongodb.MongoGridFSException;
import com.mongodb.client.MongoClient;
import io.quarkus.mongodb.MongoClientName;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mongodb.gridfs.GridFsConstants;

@Path("/mongodb-gridfs")
public class MongodbGridfsResource {

    static final String DEFAULT_MONGO_CLIENT_NAME = "camelMongoClient";
    static final String NAMED_MONGO_CLIENT_NAME = "myMongoClient";

    @Inject
    @MongoClientName(value = NAMED_MONGO_CLIENT_NAME)
    MongoClient namedMongoClient;

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/upload/{fileName}")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response uploadFile(@PathParam("fileName") String fileName, String content,
            @HeaderParam("mongoClientName") String mongoClientName) throws Exception {
        Map<String, Object> headers = new HashMap<>();
        headers.put(Exchange.FILE_NAME, fileName);
        headers.put(Exchange.CONTENT_TYPE, "text/plain");

        Exchange result = producerTemplate.request(
                String.format("mongodb-gridfs:%s?database=test&operation=create", mongoClientName),
                exchange -> {
                    exchange.getMessage().setHeaders(headers);
                    exchange.getMessage().setBody(content);
                });

        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(result.getMessage().getHeader(GridFsConstants.GRIDFS_OBJECT_ID))
                .build();
    }

    @Path("/get/{fileName}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response retrieveFile(@PathParam("fileName") String fileName,
            @HeaderParam("mongoClientName") String mongoClientName) {
        String result = null;
        try {
            result = producerTemplate.requestBodyAndHeader(
                    String.format("mongodb-gridfs:%s?database=test&operation=findOne", mongoClientName),
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
    public Response deleteFile(@PathParam("fileName") String fileName,
            @HeaderParam("mongoClientName") String mongoClientName) {
        producerTemplate.requestBodyAndHeader(
                String.format("mongodb-gridfs:camelMongoClient?database=test&operation=remove", mongoClientName),
                null,
                Exchange.FILE_NAME,
                fileName);
        return Response.noContent().build();
    }
}
