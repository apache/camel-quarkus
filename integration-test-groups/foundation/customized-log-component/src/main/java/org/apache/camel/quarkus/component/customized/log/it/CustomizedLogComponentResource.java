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
package org.apache.camel.quarkus.component.customized.log.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.log.LogComponent;
import org.apache.camel.spi.Registry;
import org.apache.camel.support.processor.DefaultExchangeFormatter;

@Path("/customized-log-component")
@ApplicationScoped
public class CustomizedLogComponentResource {
    @Inject
    Registry registry;

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/exchange-formatter/{key}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String exchangeFormatterConfig(@PathParam("key") String key) {
        LogComponent component = registry.lookupByNameAndType("log", LogComponent.class);
        DefaultExchangeFormatter def = (DefaultExchangeFormatter) component.getExchangeFormatter();

        switch (key) {
        case "show-all":
            return String.valueOf(def.isShowAll());
        case "multi-line":
            return String.valueOf(def.isMultiline());
        default:
            throw new IllegalStateException("Unexpected key " + key);
        }
    }

}
