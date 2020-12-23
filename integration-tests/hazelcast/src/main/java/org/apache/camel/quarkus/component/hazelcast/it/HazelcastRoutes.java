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
package org.apache.camel.quarkus.component.hazelcast.it;

import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Produces;

import com.hazelcast.core.HazelcastInstance;
import io.quarkus.arc.Unremovable;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.hazelcast.HazelcastConstants;
import org.apache.camel.component.hazelcast.HazelcastDefaultComponent;
import org.apache.camel.component.hazelcast.atomicnumber.HazelcastAtomicnumberComponent;
import org.apache.camel.component.hazelcast.instance.HazelcastInstanceComponent;
import org.apache.camel.component.hazelcast.list.HazelcastListComponent;
import org.apache.camel.component.hazelcast.map.HazelcastMapComponent;
import org.apache.camel.component.hazelcast.multimap.HazelcastMultimapComponent;
import org.apache.camel.component.hazelcast.policy.HazelcastRoutePolicy;
import org.apache.camel.component.hazelcast.queue.HazelcastQueueComponent;
import org.apache.camel.component.hazelcast.replicatedmap.HazelcastReplicatedmapComponent;
import org.apache.camel.component.hazelcast.ringbuffer.HazelcastRingbufferComponent;
import org.apache.camel.component.hazelcast.seda.HazelcastSedaComponent;
import org.apache.camel.component.hazelcast.set.HazelcastSetComponent;
import org.apache.camel.component.hazelcast.topic.HazelcastTopicComponent;
import org.apache.camel.processor.idempotent.hazelcast.HazelcastIdempotentRepository;
import org.apache.camel.spi.RoutePolicy;

@ApplicationScoped
public class HazelcastRoutes extends RouteBuilder {
    public static final String MOCK_LIST_ADDED = "mock:list-added";
    public static final String MOCK_LIST_DELETED = "mock:list-removed";
    public static final String MOCK_SET_ADDED = "mock:set-added";
    public static final String MOCK_SET_DELETED = "mock:set-removed";
    public static final String MOCK_MAP_ADDED = "mock:map-added";
    public static final String MOCK_MAP_DELETED = "mock:map-removed";
    public static final String MOCK_MAP_UPDATED = "mock:map-updated";
    public static final String MOCK_MAP_EVICTED = "mock:map-evicted";
    public static final String MOCK_MULTIMAP_ADDED = "mock:multimap-added";
    public static final String MOCK_MULTIMAP_DELETED = "mock:multimap-removed";
    public static final String MOCK_REPLICATED_ADDED = "mock:replicatedmap-added";
    public static final String MOCK_REPLICATED_DELETED = "mock:replicatedmap-removed";
    public static final String MOCK_TOPIC_RECEIVED = "mock:topic-received";
    public static final String MOCK_QUEUE_ADDED = "mock:queue-listen-added";
    public static final String MOCK_QUEUE_DELETED = "mock:queue-listen-removed";
    public static final String MOCK_QUEUE_POLL = "mock:queue-poll-result";
    public static final String MOCK_SEDA_FIFO = "mock:seda-fifo";
    public static final String MOCK_SEDA_IN_ONLY = "mock:seda-in-only";
    public static final String MOCK_SEDA_IN_OUT = "mock:seda-in-out";
    public static final String MOCK_SEDA_IN_OUT_TRANSACTED = "mock:seda-in-out-trans";
    public static final String MOCK_INSTANCE_ADDED = "mock:instance-added";
    public static final String MOCK_INSTANCE_REMOVED = "mock:instance-removed";
    public static final String MOCK_IDEMPOTENT_ADDED = "mock:idempotent-added";
    public static final String MOCK_POLICY = "mock:policy";

    @Inject
    HazelcastInstance hazelcastInstance;

    @Produces
    @ApplicationScoped
    @Unremovable
    @Named("hazelcast-instance")
    HazelcastDefaultComponent hazelcastInstance() {
        final HazelcastInstanceComponent hazelcastComponent = new HazelcastInstanceComponent();
        return configureHazelcastComponent(hazelcastComponent);
    }

    @Produces
    @ApplicationScoped
    @Unremovable
    @Named("hazelcast-atomicvalue")
    HazelcastDefaultComponent hazelcastAtomicnumber() {
        final HazelcastAtomicnumberComponent hazelcastComponent = new HazelcastAtomicnumberComponent();
        return configureHazelcastComponent(hazelcastComponent);
    }

    @Produces
    @ApplicationScoped
    @Unremovable
    @Named("hazelcast-list")
    HazelcastDefaultComponent hazelcastList() {
        final HazelcastListComponent hazelcastComponent = new HazelcastListComponent();
        return configureHazelcastComponent(hazelcastComponent);
    }

    @Produces
    @ApplicationScoped
    @Unremovable
    @Named("hazelcast-map")
    HazelcastDefaultComponent hazelcastMap() {
        final HazelcastMapComponent hazelcastComponent = new HazelcastMapComponent();
        return configureHazelcastComponent(hazelcastComponent);
    }

    @Produces
    @ApplicationScoped
    @Unremovable
    @Named("hazelcast-multimap")
    HazelcastDefaultComponent hazelcastMultimap() {
        final HazelcastMultimapComponent hazelcastComponent = new HazelcastMultimapComponent();
        return configureHazelcastComponent(hazelcastComponent);
    }

    @Produces
    @ApplicationScoped
    @Unremovable
    @Named("hazelcast-replicatedmap")
    HazelcastDefaultComponent hazelcastReplicatedmap() {
        final HazelcastReplicatedmapComponent hazelcastComponent = new HazelcastReplicatedmapComponent();
        return configureHazelcastComponent(hazelcastComponent);
    }

    @Produces
    @ApplicationScoped
    @Unremovable
    @Named("hazelcast-set")
    HazelcastDefaultComponent hazelcastSet() {
        final HazelcastSetComponent hazelcastComponent = new HazelcastSetComponent();
        return configureHazelcastComponent(hazelcastComponent);
    }

    @Produces
    @ApplicationScoped
    @Unremovable
    @Named("hazelcast-topic")
    HazelcastDefaultComponent hazelcastTopic() {
        final HazelcastTopicComponent hazelcastComponent = new HazelcastTopicComponent();
        return configureHazelcastComponent(hazelcastComponent);
    }

    @Produces
    @ApplicationScoped
    @Unremovable
    @Named("hazelcast-queue")
    HazelcastDefaultComponent hazelcastQueue() {
        final HazelcastQueueComponent hazelcastComponent = new HazelcastQueueComponent();
        return configureHazelcastComponent(hazelcastComponent);
    }

    @Produces
    @ApplicationScoped
    @Unremovable
    @Named("hazelcast-ringbuffer")
    HazelcastDefaultComponent hazelcastRingbuffer() {
        final HazelcastRingbufferComponent hazelcastComponent = new HazelcastRingbufferComponent();
        return configureHazelcastComponent(hazelcastComponent);
    }

    @Produces
    @ApplicationScoped
    @Unremovable
    @Named("hazelcast-seda")
    HazelcastDefaultComponent hazelcastSeda() {
        final HazelcastSedaComponent hazelcastComponent = new HazelcastSedaComponent();
        return configureHazelcastComponent(hazelcastComponent);
    }

    private HazelcastDefaultComponent configureHazelcastComponent(HazelcastDefaultComponent hazelcastComponent) {
        // pass the hazelcast generated by the hazelcast extension
        hazelcastComponent.setHazelcastInstance(hazelcastInstance);
        // sets the mode to "client"
        hazelcastComponent.setHazelcastMode(HazelcastConstants.HAZELCAST_CLIENT_MODE);
        return hazelcastComponent;
    }

    @Override
    public void configure() {
        // HazelcastListConsumer
        from("hazelcast-list:foo-list").log("object...").choice()
                .when(header(HazelcastConstants.LISTENER_ACTION).isEqualTo(HazelcastConstants.ADDED))
                .log("...added").to(MOCK_LIST_ADDED)
                .when(header(HazelcastConstants.LISTENER_ACTION).isEqualTo(HazelcastConstants.REMOVED))
                .log("...removed").to(MOCK_LIST_DELETED).otherwise()
                .log("fail!");

        // HazelcastMapConsumer
        from("hazelcast-map:foo-map").log("object...").choice()
                .when(header(HazelcastConstants.LISTENER_ACTION).isEqualTo(HazelcastConstants.ADDED))
                .log("...added").to(MOCK_MAP_ADDED)
                .when(header(HazelcastConstants.LISTENER_ACTION).isEqualTo(HazelcastConstants.EVICTED))
                .log("...evicted").to(MOCK_MAP_EVICTED)
                .when(header(HazelcastConstants.LISTENER_ACTION).isEqualTo(HazelcastConstants.UPDATED))
                .log("...updated").to(MOCK_MAP_UPDATED)
                .when(header(HazelcastConstants.LISTENER_ACTION).isEqualTo(HazelcastConstants.REMOVED))
                .log("...removed").to(MOCK_MAP_DELETED)
                .otherwise().log("fail!");

        //HazelcastMultimapConsumer
        from("hazelcast-multimap:foo-multimap").log("object...").choice()
                .when(header(HazelcastConstants.LISTENER_ACTION).isEqualTo(HazelcastConstants.ADDED)).log("...added")
                .to(MOCK_MULTIMAP_ADDED)
                .when(header(HazelcastConstants.LISTENER_ACTION).isEqualTo(HazelcastConstants.REMOVED))
                .log("...removed").to(MOCK_MULTIMAP_DELETED).otherwise().log("fail!");

        //HazelcastReplicatedmapConsumer
        from("hazelcast-replicatedmap:foo-replicate").log("object...").choice()
                .when(header(HazelcastConstants.LISTENER_ACTION).isEqualTo(HazelcastConstants.ADDED)).log("...added")
                .to(MOCK_REPLICATED_ADDED)
                .when(header(HazelcastConstants.LISTENER_ACTION).isEqualTo(HazelcastConstants.REMOVED))
                .log("...removed").to(MOCK_REPLICATED_DELETED).otherwise().log("fail!");

        // HazelcastSetConsumer
        from("hazelcast-set:foo-set").log("object...").choice()
                .when(header(HazelcastConstants.LISTENER_ACTION).isEqualTo(HazelcastConstants.ADDED))
                .log("...added").to(MOCK_SET_ADDED)
                .when(header(HazelcastConstants.LISTENER_ACTION).isEqualTo(HazelcastConstants.REMOVED))
                .log("...removed").to(MOCK_SET_DELETED).otherwise()
                .log("fail!");

        //HazelcastTopicConsumer
        from("hazelcast-topic:foo-topic").log("object...")
                .choice()
                .when(header(HazelcastConstants.LISTENER_ACTION).isEqualTo(HazelcastConstants.RECEIVED))
                .log("...received").to(MOCK_TOPIC_RECEIVED)
                .otherwise()
                .log("fail!");

        // 2 different consumers of type : HazelcastQueueConsumer
        // consumer mode  : LISTEN
        from("hazelcast-queue:foo-queue").log("object...").choice()
                .when(header(HazelcastConstants.LISTENER_ACTION).isEqualTo(HazelcastConstants.ADDED))
                .log("...added").to(MOCK_QUEUE_ADDED)
                .when(header(HazelcastConstants.LISTENER_ACTION).isEqualTo(HazelcastConstants.REMOVED))
                .log("...removed").to(MOCK_QUEUE_DELETED).otherwise()
                .log("fail!");
        // consumer mode : poll
        from("hazelcast-queue:foo-queue-poll?queueConsumerMode=Poll")
                .to(MOCK_QUEUE_POLL);

        //different HazelcastSedaConsumer
        // FIFO consumer
        from("hazelcast-seda:foo-fifo")
                .to(MOCK_SEDA_FIFO);
        // IN ONLY consumer
        from("hazelcast-seda:foo-in-only")
                .to(MOCK_SEDA_IN_ONLY);
        // IN OUT consumer
        from("hazelcast-seda:foo-in-out")
                .to(MOCK_SEDA_IN_OUT);
        // IN OUT transacted consumer
        from("hazelcast-seda:foo-in-out-trans?transacted=true")
                .to(MOCK_SEDA_IN_OUT_TRANSACTED);

        // HazelcastInstanceConsumer
        from("hazelcast-instance:foo-instance").log("instance...")
                .choice()
                .when(header(HazelcastConstants.LISTENER_ACTION).isEqualTo(HazelcastConstants.ADDED))
                .log("...added").to(MOCK_INSTANCE_ADDED)
                .otherwise().log("...removed").to(MOCK_INSTANCE_REMOVED);

        // Idempotent Repository
        HazelcastIdempotentRepository repo = new HazelcastIdempotentRepository(hazelcastInstance, "myRepo");
        from("direct:in-idempotent")
                .idempotentConsumer(header("messageId"), repo)
                .to(MOCK_IDEMPOTENT_ADDED);

        // route policy
        from("direct:in-policy")
                .routeId("id-value")
                .routePolicy(createRoutePolicy())
                .to(MOCK_POLICY);

    }

    /**
     * Creates a RoutePolicy
     * 
     */
    private RoutePolicy createRoutePolicy() {
        HazelcastRoutePolicy policy = new HazelcastRoutePolicy(hazelcastInstance);
        policy.setLockMapName("camel:lock:map");
        policy.setLockKey("route-policy");
        policy.setLockValue("id-value");
        policy.setTryLockTimeout(5, TimeUnit.SECONDS);
        return policy;
    }
}
