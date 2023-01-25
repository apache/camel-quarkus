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
package org.apache.camel.quarkus.kotlin

import org.apache.camel.CamelContext
import org.apache.camel.Route
import java.util.stream.Collectors
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.json.Json
import jakarta.json.JsonObject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType


@Path("/test")
@ApplicationScoped
class KotlinResource {
    @Inject
    var context: CamelContext? = null

    @Path("/describe-context")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun describeMain(): JsonObject? {
        val routes = context!!.routes.stream().map { obj: Route -> obj.id }.collect(Collectors.toList())

        return Json.createObjectBuilder()
                .add("routes", Json.createArrayBuilder(routes))
                .build()
    }
}