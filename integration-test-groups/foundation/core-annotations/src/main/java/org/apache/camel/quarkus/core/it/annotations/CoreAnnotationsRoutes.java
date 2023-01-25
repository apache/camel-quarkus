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
package org.apache.camel.quarkus.core.it.annotations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.camel.EndpointInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.direct.DirectEndpoint;

@ApplicationScoped
public class CoreAnnotationsRoutes extends RouteBuilder {

    @Inject
    @Named("results")
    Map<String, List<String>> results;

    @EndpointInject("direct:endpointInjectDirect1")
    DirectEndpoint endpointInjectDirect1;
    @EndpointInject("direct:endpointInjectDirect2")
    DirectEndpoint endpointInjectDirect2;

    @Override
    public void configure() {

        from("direct:endpointInjectTemplate")
                .id("endpointInjectTemplate")
                .process(e -> results.get("endpointInjectTemplate").add(e.getMessage().getBody(String.class)));

        from("direct:endpointInjectFluentTemplate")
                .process(e -> results.get("endpointInjectFluentTemplate").add(e.getMessage().getBody(String.class)));

        from("direct:produceProducer")
                .process(e -> results.get("produceProducer").add(e.getMessage().getBody(String.class)));

        from("direct:produceProducerFluent")
                .process(e -> results.get("produceProducerFluent").add(e.getMessage().getBody(String.class)));

        from("direct:endpointInjectDirectStart1")
                .to(endpointInjectDirect1);
        from("direct:endpointInjectDirect1")
                .process(e -> results.get("endpointInjectDirect1").add(e.getMessage().getBody(String.class)));

        from("direct:endpointInjectDirectStart2")
                .to(endpointInjectDirect2);
        from("direct:endpointInjectDirect2")
                .process(e -> results.get("endpointInjectDirect2").add(e.getMessage().getBody(String.class)));

    }

    @Produces
    @ApplicationScoped
    @Named("results")
    public Map<String, List<String>> results() {
        Map<String, List<String>> result = new HashMap<>();
        result.put("endpointInjectTemplate", new CopyOnWriteArrayList<>());
        result.put("endpointInjectFluentTemplate", new CopyOnWriteArrayList<>());
        result.put("endpointInjectDirect1", new CopyOnWriteArrayList<>());
        result.put("endpointInjectDirect2", new CopyOnWriteArrayList<>());
        result.put("produceProducer", new CopyOnWriteArrayList<>());
        result.put("produceProducerFluent", new CopyOnWriteArrayList<>());
        return result;
    }

}
