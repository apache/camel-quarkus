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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.hazelcast.collection.ItemEvent;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.topic.impl.DataAwareMessage;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
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
import org.jboss.logging.Logger;

@ApplicationScoped
public class HazelcastRoutes extends RouteBuilder {
    private static final Logger LOG = Logger.getLogger(HazelcastRoutes.class);

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

    @Inject
    @Named("hazelcastResults")
    Map<String, List<String>> hazelcastResults;

    @Named("hazelcast-instance")
    HazelcastDefaultComponent hazelcastInstance() {
        final HazelcastInstanceComponent hazelcastComponent = new HazelcastInstanceComponent();
        return configureHazelcastComponent(hazelcastComponent);
    }

    @Named("hazelcast-atomicvalue")
    HazelcastDefaultComponent hazelcastAtomicnumber() {
        final HazelcastAtomicnumberComponent hazelcastComponent = new HazelcastAtomicnumberComponent();
        return configureHazelcastComponent(hazelcastComponent);
    }

    @Named("hazelcast-list")
    HazelcastDefaultComponent hazelcastList() {
        final HazelcastListComponent hazelcastComponent = new HazelcastListComponent();
        return configureHazelcastComponent(hazelcastComponent);
    }

    @Named("hazelcast-map")
    HazelcastDefaultComponent hazelcastMap() {
        final HazelcastMapComponent hazelcastComponent = new HazelcastMapComponent();
        return configureHazelcastComponent(hazelcastComponent);
    }

    @Named("hazelcast-multimap")
    HazelcastDefaultComponent hazelcastMultimap() {
        final HazelcastMultimapComponent hazelcastComponent = new HazelcastMultimapComponent();
        return configureHazelcastComponent(hazelcastComponent);
    }

    @Named("hazelcast-replicatedmap")
    HazelcastDefaultComponent hazelcastReplicatedmap() {
        final HazelcastReplicatedmapComponent hazelcastComponent = new HazelcastReplicatedmapComponent();
        return configureHazelcastComponent(hazelcastComponent);
    }

    @Named("hazelcast-set")
    HazelcastDefaultComponent hazelcastSet() {
        final HazelcastSetComponent hazelcastComponent = new HazelcastSetComponent();
        return configureHazelcastComponent(hazelcastComponent);
    }

    @Named("hazelcast-topic")
    HazelcastDefaultComponent hazelcastTopic() {
        final HazelcastTopicComponent hazelcastComponent = new HazelcastTopicComponent();
        return configureHazelcastComponent(hazelcastComponent);
    }

    @Named("hazelcast-queue")
    HazelcastDefaultComponent hazelcastQueue() {
        final HazelcastQueueComponent hazelcastComponent = new HazelcastQueueComponent();
        return configureHazelcastComponent(hazelcastComponent);
    }

    @Named("hazelcast-ringbuffer")
    HazelcastDefaultComponent hazelcastRingbuffer() {
        final HazelcastRingbufferComponent hazelcastComponent = new HazelcastRingbufferComponent();
        return configureHazelcastComponent(hazelcastComponent);
    }

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
        from("hazelcast-list:foo-list")
                .process(e -> LOG.info("object..."))
                .choice()
                .when(header(HazelcastConstants.LISTENER_ACTION).isEqualTo(HazelcastConstants.ADDED))
                .process(e -> LOG.info("...added"))
                .process(new ItemEventCollector(hazelcastResults, MOCK_LIST_ADDED))
                .when(header(HazelcastConstants.LISTENER_ACTION).isEqualTo(HazelcastConstants.REMOVED))
                .process(e -> LOG.info("...removed"))
                .process(new ItemEventCollector(hazelcastResults, MOCK_LIST_DELETED))
                .otherwise()
                .process(e -> LOG.info("fail!"));

        // HazelcastMapConsumer
        from("hazelcast-map:foo-map")
                .process(e -> LOG.info("object..."))
                .choice()
                .when(header(HazelcastConstants.LISTENER_ACTION).isEqualTo(HazelcastConstants.ADDED))
                .process(e -> LOG.info("...added"))
                .process(new ObjectIdCollector(hazelcastResults, MOCK_MAP_ADDED))
                .when(header(HazelcastConstants.LISTENER_ACTION).isEqualTo(HazelcastConstants.EVICTED))
                .process(e -> LOG.info("...evicted"))
                .process(new ObjectIdCollector(hazelcastResults, MOCK_MAP_EVICTED))
                .when(header(HazelcastConstants.LISTENER_ACTION).isEqualTo(HazelcastConstants.UPDATED))
                .process(e -> LOG.info("...updated"))
                .process(new ObjectIdCollector(hazelcastResults, MOCK_MAP_UPDATED))
                .when(header(HazelcastConstants.LISTENER_ACTION).isEqualTo(HazelcastConstants.REMOVED))
                .process(e -> LOG.info("...removed"))
                .process(new ObjectIdCollector(hazelcastResults, MOCK_MAP_DELETED))
                .otherwise()
                .process(e -> LOG.info("fail!"));

        // HazelcastMultimapConsumer
        from("hazelcast-multimap:foo-multimap")
                .process(e -> LOG.info("object..."))
                .choice()
                .when(header(HazelcastConstants.LISTENER_ACTION).isEqualTo(HazelcastConstants.ADDED))
                .process(e -> LOG.info("...added"))
                .process(new ObjectIdCollector(hazelcastResults, MOCK_MULTIMAP_ADDED))
                .when(header(HazelcastConstants.LISTENER_ACTION).isEqualTo(HazelcastConstants.REMOVED))
                .process(e -> LOG.info("...removed"))
                .process(new ObjectIdCollector(hazelcastResults, MOCK_MULTIMAP_DELETED))
                .otherwise()
                .process(e -> LOG.info("fail!"));

        // HazelcastReplicatedmapConsumer
        from("hazelcast-replicatedmap:foo-replicate")
                .process(e -> LOG.info("object..."))
                .choice()
                .when(header(HazelcastConstants.LISTENER_ACTION).isEqualTo(HazelcastConstants.ADDED))
                .process(e -> LOG.info("...added"))
                .process(new ObjectIdCollector(hazelcastResults, MOCK_REPLICATED_ADDED))
                .when(header(HazelcastConstants.LISTENER_ACTION).isEqualTo(HazelcastConstants.REMOVED))
                .process(e -> LOG.info("...removed"))
                .process(new ObjectIdCollector(hazelcastResults, MOCK_REPLICATED_DELETED))
                .otherwise()
                .process(e -> LOG.info("fail!"));

        // HazelcastSetConsumer
        from("hazelcast-set:foo-set")
                .process(e -> LOG.info("object..."))
                .choice()
                .when(header(HazelcastConstants.LISTENER_ACTION).isEqualTo(HazelcastConstants.ADDED))
                .process(e -> LOG.info("...added"))
                .process(new ItemEventCollector(hazelcastResults, MOCK_SET_ADDED))
                .when(header(HazelcastConstants.LISTENER_ACTION).isEqualTo(HazelcastConstants.REMOVED))
                .process(e -> LOG.info("...removed"))
                .process(new ItemEventCollector(hazelcastResults, MOCK_SET_DELETED))
                .otherwise()
                .process(e -> LOG.info("fail!"));

        // HazelcastTopicConsumer
        from("hazelcast-topic:foo-topic")
                .process(e -> LOG.info("object..."))
                .choice()
                .when(header(HazelcastConstants.LISTENER_ACTION).isEqualTo(HazelcastConstants.RECEIVED))
                .process(e -> LOG.info("...received"))
                .process(new DataAwareCollector(hazelcastResults, MOCK_TOPIC_RECEIVED))
                .otherwise()
                .process(e -> LOG.info("fail!"));

        // 2 different consumers of type : HazelcastQueueConsumer
        // consumer mode : LISTEN
        from("hazelcast-queue:foo-queue")
                .process(e -> LOG.info("object..."))
                .choice()
                .when(header(HazelcastConstants.LISTENER_ACTION).isEqualTo(HazelcastConstants.ADDED))
                .process(e -> LOG.info("...added"))
                .process(new ItemEventCollector(hazelcastResults, MOCK_QUEUE_ADDED))
                .when(header(HazelcastConstants.LISTENER_ACTION).isEqualTo(HazelcastConstants.REMOVED))
                .process(e -> LOG.info("...removed"))
                .process(new ItemEventCollector(hazelcastResults, MOCK_QUEUE_DELETED))
                .otherwise()
                .process(e -> LOG.info("fail!"));
        // consumer mode : poll
        from("hazelcast-queue:foo-queue-poll?queueConsumerMode=Poll")
                .process(new StringBodyCollector(hazelcastResults, MOCK_QUEUE_POLL));

        // different HazelcastSedaConsumer
        // FIFO consumer
        from("hazelcast-seda:foo-fifo")
                .process(new StringBodyCollector(hazelcastResults, MOCK_SEDA_FIFO));
        // IN ONLY consumer
        from("hazelcast-seda:foo-in-only")
                .process(new StringBodyCollector(hazelcastResults, MOCK_SEDA_IN_ONLY));
        // IN OUT consumer
        from("hazelcast-seda:foo-in-out")
                .process(new StringBodyCollector(hazelcastResults, MOCK_SEDA_IN_OUT));
        // IN OUT transacted consumer
        from("hazelcast-seda:foo-in-out-trans?transacted=true")
                .process(new StringBodyCollector(hazelcastResults, MOCK_SEDA_IN_OUT_TRANSACTED));

        // HazelcastInstanceConsumer
        from("hazelcast-instance:foo-instance")
                .process(e -> LOG.info("instance..."))
                .choice()
                .when(header(HazelcastConstants.LISTENER_ACTION).isEqualTo(HazelcastConstants.ADDED))
                .process(e -> LOG.info("...added"))
                .process(new ObjectIdCollector(hazelcastResults, MOCK_INSTANCE_ADDED))
                .otherwise()
                .process(e -> LOG.info("...removed"))
                .process(new ObjectIdCollector(hazelcastResults, MOCK_INSTANCE_REMOVED));

        // Idempotent Repository
        HazelcastIdempotentRepository repo = new HazelcastIdempotentRepository(hazelcastInstance, "myRepo");
        from("direct:in-idempotent")
                .idempotentConsumer(header("messageId"), repo)
                .process(new StringBodyCollector(hazelcastResults, MOCK_IDEMPOTENT_ADDED));

        // route policy
        from("direct:in-policy")
                .routeId("id-value")
                .routePolicy(createRoutePolicy())
                .process(new StringBodyCollector(hazelcastResults, MOCK_POLICY));

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

    static class Producers {
        @javax.enterprise.inject.Produces
        @Singleton
        @Named("hazelcastResults")
        Map<String, List<String>> hazelcastResults() {
            return new ConcurrentHashMap<>();
        }
    }

    static class ObjectIdCollector implements Processor {
        private final List<String> results;

        public ObjectIdCollector(Map<String, List<String>> hazelcastResults, String key) {
            final List<String> r = new CopyOnWriteArrayList<>();
            hazelcastResults.put(key, r);
            this.results = r;
        }

        @Override
        public void process(Exchange exchange) throws Exception {
            final String val = exchange.getMessage().getHeader(HazelcastConstants.OBJECT_ID, String.class);
            results.add(val);
        }
    }

    static class StringBodyCollector implements Processor {
        private final List<String> results;

        public StringBodyCollector(Map<String, List<String>> hazelcastResults, String key) {
            final List<String> r = new CopyOnWriteArrayList<>();
            hazelcastResults.put(key, r);
            this.results = r;
        }

        @Override
        public void process(Exchange exchange) throws Exception {
            final String val = exchange.getMessage().getBody(String.class);
            results.add(val);
        }
    }

    static class ItemEventCollector implements Processor {
        private final List<String> results;

        public ItemEventCollector(Map<String, List<String>> hazelcastResults, String key) {
            final List<String> r = new CopyOnWriteArrayList<>();
            hazelcastResults.put(key, r);
            this.results = r;
        }

        @Override
        public void process(Exchange exchange) throws Exception {
            final String val = (String) exchange.getMessage().getBody(ItemEvent.class).getItem();
            results.add(val);
        }
    }

    static class DataAwareCollector implements Processor {
        private final List<String> results;

        public DataAwareCollector(Map<String, List<String>> hazelcastResults, String key) {
            final List<String> r = new CopyOnWriteArrayList<>();
            hazelcastResults.put(key, r);
            this.results = r;
        }

        @Override
        public void process(Exchange exchange) throws Exception {
            final String val = (String) exchange.getMessage().getBody(DataAwareMessage.class).getMessageObject();
            results.add(val);
        }
    }
}
