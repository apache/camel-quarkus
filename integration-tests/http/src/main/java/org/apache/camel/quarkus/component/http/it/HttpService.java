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
package org.apache.camel.quarkus.component.http.it;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/service")
@ApplicationScoped
public class HttpService {
    @Path("/get")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String get() {
        return "get";
    }

    @Path("/toUpper")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String toUpper(String message) {
        return message.toUpperCase();
    }

    @POST
    @Path("/multipart-form-params")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public String multipartFormParams(@FormParam("organization") String organization, @FormParam("project") String project) {
        return String.format("multipartFormParams(%s, %s)", organization, project);
    }

    @POST
    @Path("/multipart-form-data")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    public String multipartFormData(Map<String, String> parts) {
        if (parts.size() != 2 || !parts.keySet().contains("part-1") || !parts.keySet().contains("part-2")) {
            throw new IllegalArgumentException(
                    "There should be exactly 2 parts named \"part-1\" and \"parts-2\" in the multipart upload");
        }
        return String.format("multipartFormData(%s, %s)", parts.get("part-1"), parts.get("part-2"));
    }
}
