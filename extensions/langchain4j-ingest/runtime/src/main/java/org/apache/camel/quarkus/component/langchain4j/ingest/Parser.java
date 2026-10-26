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

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** The supported document parsers, each with the action Kamelet performing the parse. */
enum Parser {
    TIKA("tika-extract-text-action"),
    DOCLING("docling-convert-action");

    // derived from the constants, so a parser added here cannot diverge from the validated set
    static final Set<String> NAMES = Arrays.stream(values())
            .map(parser -> parser.name().toLowerCase(Locale.ROOT))
            .collect(Collectors.toUnmodifiableSet());

    final String actionKamelet;

    Parser(String actionKamelet) {
        this.actionKamelet = actionKamelet;
    }
}
