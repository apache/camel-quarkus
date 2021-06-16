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
package org.apache.camel.quarkus.component.infinispan;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.infinispan.remote.InfinispanRemoteComponent;
import org.infinispan.client.hotrod.RemoteCacheManager;

@Path("/test")
@ApplicationScoped
public class InfinispanResources {
    public static final String CACHE_NAME = "camel";

    @Inject
    RemoteCacheManager cacheManager;

    @Inject
    ProducerTemplate template;

    @Inject
    CamelContext camelContext;

    @PostConstruct
    public void setUp() {
        cacheManager.administration().getOrCreateCache(CACHE_NAME, (String) null);
    }

    @Path("/inspect")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject inspectCamelInfinispanClientConfiguration() {
        InfinispanRemoteComponent component = camelContext.getComponent("infinispan", InfinispanRemoteComponent.class);

        return Json.createObjectBuilder()
                .add("hosts", component.getConfiguration().getHosts())
                .add("cache-manager", Objects.toString(component.getConfiguration().getCacheContainer(), "none"))
                .build();
    }

    @Path("/get")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String get(@QueryParam("component") String component) {
        Map<String, Object> headers = getCommonHeaders(component);
        return template.requestBodyAndHeaders("direct:get", "", headers, String.class);
    }

    @Path("/put")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String put(@QueryParam("component") String component, String content) {
        Map<String, Object> headers = getCommonHeaders(component);
        return template.requestBodyAndHeaders("direct:put", content, headers, String.class);
    }

    private Map<String, Object> getCommonHeaders(String componentName) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("component", componentName);
        headers.put("cacheName", CACHE_NAME);
        return headers;
    }
}
