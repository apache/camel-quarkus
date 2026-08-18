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
package org.apache.camel.quarkus.component.langchain4j.ingest;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.CamelContext;

@Recorder
public class Langchain4jIngestRecorder {

    /** @param flatEntries triples: name, class name, method name */
    public RuntimeValue<IngestBuilderPipelines> createBuilderPipelines(List<String> flatEntries) {
        List<IngestBuilderPipelines.Entry> entries = new ArrayList<>(flatEntries.size() / 3);
        for (int i = 0; i < flatEntries.size(); i += 3) {
            entries.add(new IngestBuilderPipelines.Entry(
                    flatEntries.get(i), flatEntries.get(i + 1), flatEntries.get(i + 2)));
        }
        return new RuntimeValue<>(new IngestBuilderPipelines(entries));
    }

    /**
     * Recorded as a pre-start Camel runtime task, so it runs before Camel Main binds
     * {@code camel.component.*} properties — see {@link IngestComponentPresence}.
     */
    public void checkComponentsPresent(RuntimeValue<CamelContext> camelContext) {
        ArcContainer container = Arc.container();
        IngestComponentPresence.check(
                camelContext.getValue(),
                container.instance(IngestBuildTimeConfig.class).get(),
                container.instance(IngestRunTimeConfig.class).get(),
                container.instance(IngestBuilderPipelines.class).get());
    }
}
