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
package org.apache.camel.quarkus.component.geocoder.it;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.apache.commons.io.IOUtils;

/**
 * Stubs nominatim API responses in absence of WireMock support:
 *
 * https://github.com/apache/camel-quarkus/issues/2033
 */
@Path("/fake/nominatim/api")
public class FakeNominatimApi {

    @GET
    @Path("/reverse")
    @Produces("application/json")
    public String reverse() throws IOException {
        InputStream resource = FakeNominatimApi.class.getResourceAsStream("/nominatimReverse.json");
        return IOUtils.toString(resource, StandardCharsets.UTF_8);
    }

    @GET
    @Path("/search")
    @Produces("application/json")
    public String search() throws IOException {
        InputStream resource = FakeNominatimApi.class.getResourceAsStream("/nominatimSearch.json");
        return IOUtils.toString(resource, StandardCharsets.UTF_8);
    }
}
