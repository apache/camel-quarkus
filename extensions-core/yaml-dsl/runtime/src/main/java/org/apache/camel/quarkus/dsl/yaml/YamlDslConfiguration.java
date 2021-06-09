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

package org.apache.camel.quarkus.dsl.yaml;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "camel.yaml", phase = ConfigPhase.BUILD_TIME)
public class YamlDslConfiguration {
    /**
     * If {@code true} the YAML DSL support flow-mode which allow to write more concise routes as for EIPs that have
     * their own output like filter, aggregate, split, etc. the {@code steps} element can be omitted an in that case,
     * the next processing step is automatically wired to the EIP's outputs.
     * <p/>
     * As example, a YAML DSL to process only the timer events from 5 to 10 would look like:
     *
     * <pre>
     * {@code
     * - from:
     *     uri: "timer:tick"
     *     steps:
     *       - filter:
     *           simple: "${exchangeProperty.CamelTimerCounter} range '5..10'"
     *           steps:
     *             - to: "direct:filtered"
     * }
     * </pre>
     *
     * With the flow mode enabled the same logic can be expressed in a more concise way:
     *
     * <pre>
     * {@code
     * - from:
     *     uri: "kamelet:source"
     *     steps:
     *       - filter:
     *           simple: "${exchangeProperty.CamelTimerCounter} range '5..10'"
     *       - to: "kamelet:sink"
     * }
     * </pre>
     *
     *
     */
    @ConfigItem(defaultValue = "true")
    public boolean flowMode;
}
