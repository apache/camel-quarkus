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

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.inject.spi.CDI;

/**
 * The {@link Ingest @Ingest} methods discovered at build time: (pipeline name, declaring bean
 * class, method name).
 */
public class IngestBuilderPipelines {

    public record Entry(String name, String className, String methodName) {
    }

    private final List<Entry> entries;
    private final Map<String, IngestPipeline> definitions = new ConcurrentHashMap<>();

    public IngestBuilderPipelines(List<Entry> entries) {
        this.entries = entries;
    }

    public List<Entry> entries() {
        return entries;
    }

    /**
     * The pipeline an entry's method returns, memoized: the pre-start component check and the
     * route builder both need it, and the {@code @Ingest} contract promises the method runs
     * exactly once.
     *
     * <p>
     * Reflective invocation follows the {@code @Consume} pattern with its costs accepted: the
     * method is validated at build time, registered for reflection, and run once at startup.
     */
    IngestPipeline definition(Entry entry) {
        return definitions.computeIfAbsent(entry.name(), key -> invoke(entry));
    }

    private static IngestPipeline invoke(Entry entry) {
        try {
            Class<?> declaringClass = Thread.currentThread().getContextClassLoader()
                    .loadClass(entry.className());
            Object bean = CDI.current().select(declaringClass).get();
            Method method = declaringClass.getDeclaredMethod(entry.methodName());
            method.setAccessible(true);
            return (IngestPipeline) method.invoke(bean);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to invoke @Ingest method " + entry.className() + "#"
                    + entry.methodName() + " for pipeline '" + entry.name() + "'", e);
        }
    }
}
