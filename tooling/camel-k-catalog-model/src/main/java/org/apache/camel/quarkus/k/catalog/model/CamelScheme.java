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
package org.apache.camel.quarkus.k.catalog.model;

import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;
import java.util.SortedSet;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = CamelScheme.Builder.class)
public interface CamelScheme extends Comparable<CamelScheme> {
    String getId();

    @Value.Auxiliary
    @Value.Default
    default boolean http() {
        return false;
    }

    @Value.Auxiliary
    @Value.Default
    default boolean passive() {
        return false;
    }

    @Value.Auxiliary
    @Value.Default
    @Value.NaturalOrder
    default SortedSet<String> getRequiredCapabilities() {
        return Collections.emptySortedSet();
    }

    @Value.Auxiliary
    Optional<CamelScopedArtifact> getProducer();

    @Value.Auxiliary
    Optional<CamelScopedArtifact> getConsumer();

    @Override
    default int compareTo(CamelScheme o) {
        return Comparator
                .comparing(CamelScheme::getId)
                .compare(this, o);
    }

    class Builder extends ImmutableCamelScheme.Builder {
    }
}
