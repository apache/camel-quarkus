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
package org.apache.camel.quarkus.core.deployment.devui;

/**
 * Constants for Camel Dev UI page metadata.
 */
public final class CamelDevUIConstants {

    /**
     * Page metadata key for the Camel dev console id. Every {@code qwc-camel*} page must set this
     * metadata so that the console id is included in the runtime allowlist for the JSON-RPC bridge.
     */
    public static final String CONSOLE_ID_METADATA_KEY = "consoleId";

    /**
     * Sentinel value for {@link #CONSOLE_ID_METADATA_KEY} indicating the page does not use the
     * JSON-RPC console bridge (e.g. pages extending {@code LitElement} directly).
     */
    public static final String CONSOLE_ID_NONE = "none";

    /**
     * Page metadata key for allowed console options. The value is a semicolon-delimited
     * specification of allowed option keys and their permitted values.
     * Format: {@code key1=val1|val2;key2=*} where {@code *} means any value is accepted.
     * Pages that do not declare this metadata allow no options (secure default).
     */
    public static final String ALLOWED_OPTIONS_METADATA_KEY = "allowedOptions";

    private CamelDevUIConstants() {
    }
}
