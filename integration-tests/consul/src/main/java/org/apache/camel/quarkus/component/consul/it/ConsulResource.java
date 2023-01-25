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
package org.apache.camel.quarkus.component.consul.it;

import com.orbitz.consul.model.kv.ImmutableValue;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.component.consul.ConsulConstants;
import org.apache.camel.component.consul.endpoint.ConsulKeyValueActions;

@Path("/test/kv")
@ApplicationScoped
public class ConsulResource {
    @Inject
    FluentProducerTemplate producerTemplate;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String get(@QueryParam("key") String key) {
        ImmutableValue result = producerTemplate
                .withHeader(ConsulConstants.CONSUL_ACTION, ConsulKeyValueActions.GET_VALUE)
                .withHeader(ConsulConstants.CONSUL_KEY, key)
                .to("consul:kv?url={{camel.consul.test-url}}")
                .request(ImmutableValue.class);

        return result.getValueAsString().orElseThrow(IllegalStateException::new);
    }

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public void post(@QueryParam("key") String key, String value) {
        producerTemplate
                .withHeader(ConsulConstants.CONSUL_ACTION, ConsulKeyValueActions.PUT)
                .withHeader(ConsulConstants.CONSUL_KEY, key)
                .withBody(value)
                .to("consul:kv?url={{camel.consul.test-url}}")
                .send();
    }
}
