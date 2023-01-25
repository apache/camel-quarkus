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
package org.apache.camel.quarkus.component.bean;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * A {@link RouteBuilder} injected into {@link BeanResource} and thus instantiated eagerly.
 */
@ApplicationScoped
public class EagerAppScopedRouteBuilder extends RouteBuilder {

    static final AtomicInteger INSTANCE_COUNTER = new AtomicInteger(0);
    static final AtomicInteger CONFIGURE_COUNTER = new AtomicInteger(0);

    @Inject
    Counter counter;

    @ConfigProperty(name = "my.foo.property", defaultValue = "not found")
    String myFooValue;

    @PostConstruct
    public void postConstruct() {
        INSTANCE_COUNTER.incrementAndGet();
    }

    @Override
    public void addRoutesToCamelContext(CamelContext context) throws Exception {
        CONFIGURE_COUNTER.incrementAndGet();
        super.addRoutesToCamelContext(context);
    }

    @Override
    public void configure() {

        /*
         * counter and config-property should actually work without the bean extension. Doing it here because we have
         * quarkus.camel.enable-main=true in the core itest
         */
        from("direct:increment")
                .id("counter")
                .setBody(exchange -> counter.increment());
        from("direct:config-property")
                .id("config-property")
                .setBody(exchange -> "myFooValue = " + myFooValue);
    }

    public Counter getCounter() {
        return counter;
    }

}
