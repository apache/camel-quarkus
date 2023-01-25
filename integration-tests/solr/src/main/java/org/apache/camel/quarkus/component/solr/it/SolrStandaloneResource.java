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
package org.apache.camel.quarkus.component.solr.it;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/solr/standalone")
@ApplicationScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SolrStandaloneResource extends SolrCommonResource {
    @ConfigProperty(name = "solr.standalone.url", defaultValue = "localhost:8983/solr/collection1")
    String solrUrl;

    public void init(@Observes StartupEvent startupEvent) {
        solrComponentURI = String.format("solr://%s", solrUrl);
        solrClient = new HttpSolrClient.Builder(String.format("http://%s", solrUrl)).build();
    }
}
