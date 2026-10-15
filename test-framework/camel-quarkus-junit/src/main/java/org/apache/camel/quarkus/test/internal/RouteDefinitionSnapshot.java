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
package org.apache.camel.quarkus.test.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.NamedNode;
import org.apache.camel.model.FromDefinition;
import org.apache.camel.model.Model;
import org.apache.camel.model.OutputNode;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.RouteDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A snapshot of the route definitions of a {@link CamelContext}, used to undo AdviceWith modifications.
 * <p>
 * For internal use by the Camel Quarkus test framework.
 */
public final class RouteDefinitionSnapshot {
    private static final Logger LOG = LoggerFactory.getLogger(RouteDefinitionSnapshot.class);

    private final Map<String, List<ProcessorDefinition<?>>> outputs = new HashMap<>();
    /** The outputs of the nodes below each route, by route id and node identity */
    private final Map<String, Map<Object, List<ProcessorDefinition<?>>>> nestedOutputs = new HashMap<>();
    private final Map<String, Input> inputs = new HashMap<>();

    private RouteDefinitionSnapshot() {
    }

    /**
     * Takes a snapshot of the outputs and inputs of every route definition known to the given context.
     */
    public static RouteDefinitionSnapshot take(CamelContext context) {
        RouteDefinitionSnapshot snapshot = new RouteDefinitionSnapshot();
        Model model = model(context);
        if (model != null) {
            for (RouteDefinition routeDefinition : model.getRouteDefinitions()) {
                // Copy the outputs list itself, the processors are compared by identity
                snapshot.outputs.put(routeDefinition.getId(), new ArrayList<>(routeDefinition.getOutputs()));
                Map<Object, List<ProcessorDefinition<?>>> nested = new IdentityHashMap<>();
                for (ProcessorDefinition<?> output : routeDefinition.getOutputs()) {
                    collectNestedOutputs(output, nested);
                }
                snapshot.nestedOutputs.put(routeDefinition.getId(), nested);
                FromDefinition from = routeDefinition.getInput();
                if (from != null) {
                    snapshot.inputs.put(routeDefinition.getId(), new Input(from.getUri(), from.getEndpoint()));
                }
            }
        }
        return snapshot;
    }

    /**
     * Restores the outputs of modified route definitions and restarts the existing routes. Route inputs are left
     * untouched.
     *
     * @param context    the {@link CamelContext}
     * @param onModified invoked with the id of each modified route before it is restored
     */
    public void restoreOutputsAndRestart(CamelContext context, Consumer<String> onModified) {
        Model model = model(context);
        if (model == null || outputs.isEmpty()) {
            return;
        }

        for (RouteDefinition routeDefinition : model.getRouteDefinitions()) {
            String routeId = routeDefinition.getId();
            if (isOutputsModified(routeDefinition)) {
                onModified.accept(routeId);
                try {
                    context.getRouteController().stopRoute(routeId);
                    restoreOutputs(routeDefinition);
                    context.getRouteController().startRoute(routeId);
                } catch (Exception e) {
                    LOG.warn("Failed to restore route '{}' after advice", routeId, e);
                }
            }
        }
    }

    /**
     * Restores the outputs and inputs of modified route definitions and recreates their routes, so that the running
     * routes match the snapshot again.
     *
     * @param  context the {@link CamelContext}
     * @return         the ids of the restored routes
     */
    public Set<String> restoreAndReload(CamelContext context) {
        Set<String> restored = new HashSet<>();
        Model model = model(context);
        if (model == null || outputs.isEmpty()) {
            return restored;
        }

        for (RouteDefinition routeDefinition : new ArrayList<>(model.getRouteDefinitions())) {
            String routeId = routeDefinition.getId();
            if (isOutputsModified(routeDefinition) || isNestedOutputsModified(routeDefinition)
                    || isInputModified(routeDefinition)) {
                try {
                    model.removeRouteDefinition(routeDefinition);
                    restoreOutputs(routeDefinition);
                    restoreNestedOutputs(routeDefinition);
                    restoreInput(routeDefinition);
                    model.addRouteDefinition(routeDefinition);
                    restored.add(routeId);
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to restore route '" + routeId + "' after advice", e);
                }
            }
        }
        return restored;
    }

    private boolean isOutputsModified(RouteDefinition routeDefinition) {
        List<ProcessorDefinition<?>> original = outputs.get(routeDefinition.getId());
        if (original == null) {
            return false;
        }
        return !isSame(routeDefinition.getOutputs(), original);
    }

    private boolean isNestedOutputsModified(RouteDefinition routeDefinition) {
        Map<Object, List<ProcessorDefinition<?>>> nested = nestedOutputs.get(routeDefinition.getId());
        if (nested == null) {
            return false;
        }
        for (Map.Entry<Object, List<ProcessorDefinition<?>>> entry : nested.entrySet()) {
            if (!isSame(outputsOf(entry.getKey()), entry.getValue())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSame(List<ProcessorDefinition<?>> current, List<ProcessorDefinition<?>> original) {
        if (current == null || current.size() != original.size()) {
            return false;
        }
        for (int i = 0; i < current.size(); i++) {
            if (current.get(i) != original.get(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Collects the outputs of the given node and of every node below it. AdviceWith modifies the outputs of the parent
     * of the node it weaves, which is not the route for nodes inside EIPs such as filter, split or choice.
     */
    private static void collectNestedOutputs(Object node, Map<Object, List<ProcessorDefinition<?>>> nested) {
        if (nested.containsKey(node)) {
            return;
        }
        List<ProcessorDefinition<?>> nodeOutputs = outputsOf(node);
        nested.put(node, nodeOutputs != null ? new ArrayList<>(nodeOutputs) : List.of());
        if (node instanceof NamedNode namedNode) {
            // Not always the outputs: the children of a choice are its when and otherwise clauses
            for (NamedNode child : namedNode.getChildren()) {
                collectNestedOutputs(child, nested);
            }
        }
    }

    private static List<ProcessorDefinition<?>> outputsOf(Object node) {
        if (node instanceof ProcessorDefinition<?> processorDefinition) {
            return processorDefinition.getOutputs();
        }
        return node instanceof OutputNode outputNode ? outputNode.getOutputs() : List.of();
    }

    private boolean isInputModified(RouteDefinition routeDefinition) {
        Input original = inputs.get(routeDefinition.getId());
        FromDefinition from = routeDefinition.getInput();
        if (original == null || from == null) {
            return false;
        }
        return !Objects.equals(original.uri(), from.getUri()) || original.endpoint() != from.getEndpoint();
    }

    private void restoreOutputs(RouteDefinition routeDefinition) {
        List<ProcessorDefinition<?>> original = outputs.get(routeDefinition.getId());
        if (original != null) {
            List<ProcessorDefinition<?>> current = routeDefinition.getOutputs();
            current.clear();
            current.addAll(original);
        }
    }

    private void restoreNestedOutputs(RouteDefinition routeDefinition) {
        Map<Object, List<ProcessorDefinition<?>>> nested = nestedOutputs.get(routeDefinition.getId());
        if (nested == null) {
            return;
        }
        for (Map.Entry<Object, List<ProcessorDefinition<?>>> entry : nested.entrySet()) {
            List<ProcessorDefinition<?>> current = outputsOf(entry.getKey());
            if (current != null && !isSame(current, entry.getValue())) {
                current.clear();
                current.addAll(entry.getValue());
            }
        }
    }

    private void restoreInput(RouteDefinition routeDefinition) {
        Input original = inputs.get(routeDefinition.getId());
        FromDefinition from = routeDefinition.getInput();
        if (original != null && from != null) {
            from.setEndpoint(original.endpoint());
            from.setUri(original.uri());
        }
    }

    private static Model model(CamelContext context) {
        return context.getCamelContextExtension().getContextPlugin(Model.class);
    }

    private record Input(String uri, Endpoint endpoint) {
    }
}
