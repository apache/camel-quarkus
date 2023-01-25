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
package org.apache.camel.quarkus.component.couchdb.it;

import com.google.gson.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;

import static org.apache.camel.quarkus.component.couchdb.it.CouchdbTestDocument.fromJsonObject;

@ApplicationScoped
public class CouchDbRoute extends RouteBuilder {

    static final String COUCHDB_ENDPOINT_URI = "couchdb:http://{{camel.couchdb.test.server.authority}}/database";

    @Inject
    CouchdbResource resource;

    @Override
    public void configure() {
        from(COUCHDB_ENDPOINT_URI + "?createDatabase=true&heartbeat=100").process(e -> {
            resource.logEvent(fromJsonObject(e.getIn().getBody(JsonObject.class)));
        });
    }
}
