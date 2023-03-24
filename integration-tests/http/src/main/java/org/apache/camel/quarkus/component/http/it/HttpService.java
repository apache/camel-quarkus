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
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

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

    @GET
    @Path("/custom-vertx-options")
    public void customVertxOptions() {
        // We are not expected to pass here as the Vert.x HTTP client should throw IllegalArgumentException 
    }

    @GET
    @Path("/session-management/secure")
    @Produces(MediaType.TEXT_PLAIN)
    public String getSecuredContent(@CookieParam("sessionId") String cookie) {
        if ("my-session-id-123".equals(cookie)) {
            return "Some secret content";
        } else {
            throw new ForbiddenException("A cookie with session id is needed to access the secured content");
        }
    }

    @GET
    @Path("/session-management/login")
    @Produces(MediaType.TEXT_PLAIN)
    public Response login(@HeaderParam("username") String username, @HeaderParam("password") String password) {
        if ("my-username".equals(username) && "my-password".equals(password)) {
            NewCookie cookie = new NewCookie.Builder("sessionId")
                    .value("my-session-id-123")
                    .build();
            return Response.ok().cookie(cookie).build();
        }
        throw new ForbiddenException("Wrong username/password, no cookie will be created");
    }

}
