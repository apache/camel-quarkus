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

import java.util.Comparator;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "groupId", "artifactId", "version" })
public interface Artifact extends Comparable<Artifact> {
    String getGroupId();;

    String getArtifactId();

    Optional<String> getVersion();

    @Override
    default int compareTo(Artifact o) {
        return Comparator
                .comparing(Artifact::getGroupId)
                .thenComparing(Artifact::getArtifactId)
                .thenComparing(Artifact::getVersion, Comparator.comparing(c -> c.orElse("")))
                .compare(this, o);
    }

    static Artifact from(String groupId, String artifactId) {
        return new Artifact() {
            @Override
            public String getGroupId() {
                return groupId;
            }

            @Override
            public String getArtifactId() {
                return artifactId;
            }

            @Override
            public Optional<String> getVersion() {
                return Optional.empty();
            }
        };
    }
}
