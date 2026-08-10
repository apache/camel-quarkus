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
package org.apache.camel.quarkus.component.saxon;

import java.util.Objects;

import net.sf.saxon.ma.map.MapType;
import net.sf.saxon.value.SequenceType;
import org.graalvm.nativeimage.hosted.Feature;

/**
 * Works around a class initialization deadlock in the native image builder.
 *
 * SequenceType and MapType have mutually referencing static initializers: SequenceType.SINGLE_MAP references
 * MapType.ANY_MAP_TYPE, which in turn references SequenceType.ANY_SEQUENCE. Initializing them from a single thread is
 * safe, because class initialization is reentrant and ANY_SEQUENCE is assigned before SINGLE_MAP. Initializing them
 * concurrently deadlocks, as each thread holds one class initialization lock while waiting for the other.
 *
 * Saxon functions are registered for reflection in SaxonProcessor, and the builder performs those registrations on
 * several analysis threads, which is enough to trigger the deadlock. Initializing the cycle here, before the analysis
 * starts, keeps it on a single thread.
 */
public class SaxonClassInitializationFeature implements Feature {
    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        Objects.requireNonNull(SequenceType.ANY_SEQUENCE);
        Objects.requireNonNull(MapType.ANY_MAP_TYPE);
    }
}
