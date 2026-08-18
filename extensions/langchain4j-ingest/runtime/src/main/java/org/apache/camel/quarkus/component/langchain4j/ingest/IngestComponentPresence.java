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

import org.apache.camel.CamelContext;
import org.apache.camel.util.URISupport;
import org.jboss.logging.Logger;

/**
 * Fails a pipeline whose consumer component is not on the classpath with the same add-extension
 * hint the build gives for configured URIs.
 *
 * <p>
 * The build-time check cannot see a builder-declared pipeline — its URI is composed at startup —
 * nor a configured URI whose scheme hides behind a property placeholder. And a check inside the
 * route builder would run too late whenever a {@code camel.component.<scheme>.*} property
 * references the missing component, because Camel Main binds those properties before any route
 * builder runs and fails first with its bare classpath message. This check therefore runs as a
 * pre-start task, recorded ahead of the Camel runtime assembly, which beats Camel Main's
 * property binding.
 */
final class IngestComponentPresence {

    private static final Logger LOG = Logger.getLogger(IngestComponentPresence.class);

    private IngestComponentPresence() {
    }

    /** Walks every pipeline that consumes from a component and requires the component present. */
    static void check(CamelContext context, IngestBuildTimeConfig buildTimeConfig,
            IngestRunTimeConfig runTimeConfig, IngestBuilderPipelines builderPipelines) {
        for (var entry : buildTimeConfig.pipelines().entrySet()) {
            IngestRunTimeConfig.PipelineRunTimeConfig runtime = runTimeConfig.pipelines().get(entry.getKey());
            if (runtime != null && !runtime.enabled()) {
                continue;
            }
            String uri = entry.getValue().source().uri().orElse(null);
            if (uri != null) {
                require(context, entry.getKey(), uri);
            }
        }

        for (IngestBuilderPipelines.Entry entry : builderPipelines.entries()) {
            IngestRunTimeConfig.PipelineRunTimeConfig external = runTimeConfig.pipelines().get(entry.name());
            if (external != null && !external.enabled()) {
                // a disabled pipeline's @Ingest method must not run at all
                continue;
            }
            if (external != null && (external.source().directory().isPresent()
                    || external.source().documentId().isPresent())) {
                // the route builder refuses this conflict with its own error; invoking the
                // method here first would change which failure the user sees
                continue;
            }
            IngestPipeline definition = builderPipelines.definition(entry);
            if ("endpoint".equals(definition.sourceType())) {
                require(context, entry.name(), definition.sourceUri());
            }
        }
    }

    private static void require(CamelContext context, String name, String uri) {
        String resolved;
        try {
            resolved = context.resolvePropertyPlaceholders(uri);
        } catch (Exception e) {
            // an unresolvable placeholder gets Camel's own diagnostics at route creation
            LOG.debugf(e, "Ingestion pipeline '%s': component presence not checked, "
                    + "a placeholder in the source URI did not resolve", name);
            return;
        }
        int colon = resolved.indexOf(':');
        String scheme = colon < 1 ? resolved : resolved.substring(0, colon);
        if (context.getComponent(scheme) == null) {
            // the artifact hint is a heuristic - multi-scheme components (smtp ->
            // camel-quarkus-mail) name their extension differently
            throw new IllegalStateException("Ingestion pipeline '" + name + "' consumes from '"
                    + URISupport.sanitizeUri(uri) + "', but the Camel component '" + scheme
                    + "' is not on the classpath. Add the extension that provides it, e.g. "
                    + "org.apache.camel.quarkus:camel-quarkus-" + scheme);
        }
    }
}
