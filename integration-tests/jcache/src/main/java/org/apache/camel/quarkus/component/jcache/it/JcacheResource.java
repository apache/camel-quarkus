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
package org.apache.camel.quarkus.component.jcache.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.*;
import org.jboss.logging.Logger;

@Path("/jcache")
@ApplicationScoped
public class JcacheResource {

    private static final Logger LOG = Logger.getLogger(JcacheResource.class);

    private static final String COMPONENT_JCACHE = "jcache";
    @Inject
    CamelContext context;

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/sayHello")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String sayHello() throws Exception {
        Exchange exchange = producerTemplate.request("direct:getCachedValue", new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                exchange.getMessage().setHeader("Cache-Key", "123");
            }
        });

        String body = exchange.getMessage().getBody(String.class);
        String inCacheProperty = exchange.getProperty("In-Cache", String.class);

        String response = body + " : " + inCacheProperty;

        return response;
    }

}
