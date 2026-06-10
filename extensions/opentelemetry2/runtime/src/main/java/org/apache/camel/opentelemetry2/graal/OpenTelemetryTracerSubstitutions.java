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
package org.apache.camel.opentelemetry2.graal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.apache.camel.opentelemetry2.OpenTelemetryTracer;

/**
 * Disables native-unfriendly code meant for Camel JBang dev mode.
 * The initDevSpanExporter and initOtlpReceiver methods initialize development/debugging
 * features that are not needed in native mode and rely on dynamic features unsuitable
 * for native compilation.
 */
@TargetClass(OpenTelemetryTracer.class)
final class OpenTelemetryTracerSubstitutions {

    @Substitute
    private void initDevSpanExporter() {
        // No-op in native mode
    }

    @Substitute
    private void initOtlpReceiver() {
        // No-op in native mode
    }
}
