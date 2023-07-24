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
package org.apache.camel.quarkus.k.catalog.model.k8s.crd;

import java.util.Collections;
import java.util.SortedMap;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.apache.camel.quarkus.k.catalog.model.CamelArtifact;
import org.apache.camel.quarkus.k.catalog.model.CamelLoader;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(depluralize = true)
@JsonDeserialize(builder = CamelCatalogSpec.Builder.class)
@JsonPropertyOrder({ "runtime", "artifacts" })
public interface CamelCatalogSpec {
    RuntimeSpec getRuntime();

    @Value.Default
    @Value.NaturalOrder
    default SortedMap<String, CamelArtifact> getArtifacts() {
        return Collections.emptySortedMap();
    }

    @Value.Default
    @Value.NaturalOrder
    default SortedMap<String, CamelLoader> getLoaders() {
        return Collections.emptySortedMap();
    }

    class Builder extends ImmutableCamelCatalogSpec.Builder {
        public Builder putArtifact(CamelArtifact artifact) {
            return putArtifact(artifact.getArtifactId(), artifact);
        }

        public Builder putArtifact(String groupId, String artifactId) {
            return putArtifact(new CamelArtifact.Builder().groupId(groupId).artifactId(artifactId).build());
        }
    }
}
