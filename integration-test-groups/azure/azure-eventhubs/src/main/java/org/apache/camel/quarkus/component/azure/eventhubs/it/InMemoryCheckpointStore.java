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
// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package org.apache.camel.quarkus.component.azure.eventhubs.it;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.azure.core.util.CoreUtils;
import com.azure.core.util.logging.ClientLogger;
import com.azure.messaging.eventhubs.CheckpointStore;
import com.azure.messaging.eventhubs.models.Checkpoint;
import com.azure.messaging.eventhubs.models.PartitionOwnership;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.azure.messaging.eventhubs.implementation.ClientConstants.OWNER_ID_KEY;
import static com.azure.messaging.eventhubs.implementation.ClientConstants.PARTITION_ID_KEY;
import static com.azure.messaging.eventhubs.implementation.ClientConstants.SEQUENCE_NUMBER_KEY;

/**
 * An in-memory checkpoint store. This is primarily to test custom event positioning. Inspired by
 * https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/eventhubs/azure-messaging-eventhubs/src/samples/java/com/azure/messaging/eventhubs/SampleCheckpointStore.java
 */
public class InMemoryCheckpointStore implements CheckpointStore {
    private static final String OWNERSHIP = "ownership";
    private static final String SEPARATOR = "/";
    private static final String CHECKPOINT = "checkpoint";
    private final Map<String, PartitionOwnership> partitionOwnershipMap = new ConcurrentHashMap<>();
    private final Map<String, Checkpoint> checkpointsMap = new ConcurrentHashMap<>();
    private static final ClientLogger LOGGER = new ClientLogger(InMemoryCheckpointStore.class);

    @Override
    public Flux<PartitionOwnership> listOwnership(String fullyQualifiedNamespace, String eventHubName,
            String consumerGroup) {
        LOGGER.info("Listing partition ownership");

        String prefix = prefixBuilder(fullyQualifiedNamespace, eventHubName, consumerGroup, OWNERSHIP);
        return Flux.fromIterable(partitionOwnershipMap.keySet())
                .filter(key -> key.startsWith(prefix))
                .map(key -> partitionOwnershipMap.get(key));
    }

    private String prefixBuilder(String fullyQualifiedNamespace, String eventHubName, String consumerGroup,
            String type) {
        return new StringBuilder()
                .append(fullyQualifiedNamespace)
                .append(SEPARATOR)
                .append(eventHubName)
                .append(SEPARATOR)
                .append(consumerGroup)
                .append(SEPARATOR)
                .append(type)
                .toString()
                .toLowerCase(Locale.ROOT);
    }

    @Override
    public Flux<PartitionOwnership> claimOwnership(List<PartitionOwnership> requestedPartitionOwnerships) {
        if (CoreUtils.isNullOrEmpty(requestedPartitionOwnerships)) {
            return Flux.empty();
        }
        PartitionOwnership firstEntry = requestedPartitionOwnerships.get(0);
        String prefix = prefixBuilder(firstEntry.getFullyQualifiedNamespace(), firstEntry.getEventHubName(),
                firstEntry.getConsumerGroup(), OWNERSHIP);

        return Flux.fromIterable(requestedPartitionOwnerships)
                .filter(ownershipRequest -> {
                    final String key = prefix + SEPARATOR + ownershipRequest.getPartitionId();
                    final PartitionOwnership existing = partitionOwnershipMap.get(key);

                    if (existing == null) {
                        return true;
                    }

                    return existing.getETag().equals(ownershipRequest.getETag());
                })
                .doOnNext(partitionOwnership -> LOGGER.atInfo()
                        .addKeyValue(PARTITION_ID_KEY, partitionOwnership.getPartitionId())
                        .addKeyValue(OWNER_ID_KEY, partitionOwnership.getOwnerId())
                        .log("Ownership claimed"))
                .map(partitionOwnership -> {
                    partitionOwnership.setETag(UUID.randomUUID().toString())
                            .setLastModifiedTime(System.currentTimeMillis());

                    final String key = prefix + SEPARATOR + partitionOwnership.getPartitionId();
                    partitionOwnershipMap.put(key, partitionOwnership);

                    return partitionOwnership;
                });
    }

    @Override
    public Flux<Checkpoint> listCheckpoints(String fullyQualifiedNamespace, String eventHubName, String consumerGroup) {
        String prefix = prefixBuilder(fullyQualifiedNamespace, eventHubName, consumerGroup, CHECKPOINT);
        return Flux.fromIterable(checkpointsMap.keySet())
                .filter(key -> key.startsWith(prefix))
                .map(checkpointsMap::get);
    }

    @Override
    public Mono<Void> updateCheckpoint(Checkpoint checkpoint) {
        if (checkpoint == null) {
            return Mono.error(LOGGER.logExceptionAsError(new NullPointerException("checkpoint cannot be null")));
        }

        String prefix = prefixBuilder(checkpoint.getFullyQualifiedNamespace(), checkpoint.getEventHubName(),
                checkpoint.getConsumerGroup(), CHECKPOINT);
        checkpointsMap.put(prefix + SEPARATOR + checkpoint.getPartitionId(), checkpoint);
        LOGGER.atInfo()
                .addKeyValue(PARTITION_ID_KEY, checkpoint.getPartitionId())
                .addKeyValue(SEQUENCE_NUMBER_KEY, checkpoint.getSequenceNumber())
                .log("Updated checkpoint.");
        return Mono.empty();
    }
}
