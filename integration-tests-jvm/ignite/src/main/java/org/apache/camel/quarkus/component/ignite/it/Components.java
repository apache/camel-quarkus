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
package org.apache.camel.quarkus.component.ignite.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import org.apache.camel.Component;
import org.apache.camel.component.ignite.AbstractIgniteComponent.IgniteLifecycleMode;
import org.apache.camel.component.ignite.cache.IgniteCacheComponent;
import org.apache.camel.component.ignite.compute.IgniteComputeComponent;
import org.apache.camel.component.ignite.events.IgniteEventsComponent;
import org.apache.camel.component.ignite.idgen.IgniteIdGenComponent;
import org.apache.camel.component.ignite.messaging.IgniteMessagingComponent;
import org.apache.camel.component.ignite.queue.IgniteQueueComponent;
import org.apache.camel.component.ignite.set.IgniteSetComponent;
import org.apache.ignite.Ignite;
import org.mockito.Mockito;

/**
 * Here we produce {@code ignite-*} components with a mock {@link Ignite} instance set, so that the components are in
 * {@link IgniteLifecycleMode#USER_MANAGED} mode. In that way they do not require a real Ignite cluster to be up during
 * their {@link Component#start()}. This is sufficient for JVM-only smoke testing, but once we go native, we should
 * start testing against a real cluster.
 */
public class Components {

    @Produces
    @ApplicationScoped
    @Named("ignite-cache")
    IgniteCacheComponent igniteCacheComponent() {
        final Ignite ignite = Mockito.mock(Ignite.class);
        return IgniteCacheComponent.fromIgnite(ignite);
    }

    @Produces
    @ApplicationScoped
    @Named("ignite-compute")
    IgniteComputeComponent igniteComputeComponent() {
        final Ignite ignite = Mockito.mock(Ignite.class);
        return IgniteComputeComponent.fromIgnite(ignite);
    }

    @Produces
    @ApplicationScoped
    @Named("ignite-events")
    IgniteEventsComponent igniteEventsComponent() {
        final Ignite ignite = Mockito.mock(Ignite.class);
        return IgniteEventsComponent.fromIgnite(ignite);
    }

    @Produces
    @ApplicationScoped
    @Named("ignite-idgen")
    IgniteIdGenComponent igniteIdgenComponent() {
        final Ignite ignite = Mockito.mock(Ignite.class);
        return IgniteIdGenComponent.fromIgnite(ignite);
    }

    @Produces
    @ApplicationScoped
    @Named("ignite-messaging")
    IgniteMessagingComponent igniteMessagingComponent() {
        final Ignite ignite = Mockito.mock(Ignite.class);
        return IgniteMessagingComponent.fromIgnite(ignite);
    }

    @Produces
    @ApplicationScoped
    @Named("ignite-queue")
    IgniteQueueComponent igniteQueueComponent() {
        final Ignite ignite = Mockito.mock(Ignite.class);
        return IgniteQueueComponent.fromIgnite(ignite);
    }

    @Produces
    @ApplicationScoped
    @Named("ignite-set")
    IgniteSetComponent igniteIgniteSetComponent() {
        final Ignite ignite = Mockito.mock(Ignite.class);
        return IgniteSetComponent.fromIgnite(ignite);
    }

}
