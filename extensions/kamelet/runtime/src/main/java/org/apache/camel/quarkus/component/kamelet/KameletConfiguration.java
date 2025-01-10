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
package org.apache.camel.quarkus.component.kamelet;

import java.util.List;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "camel.kamelet", phase = ConfigPhase.BUILD_TIME)
public class KameletConfiguration {
    /**
     * Optional comma separated list of kamelet identifiers to configure for native mode support.
     * A kamelet identifier is the Kamelet file name without the .kamelet.yaml suffix.
     * <p>
     * The default value '*' will result in all discovered Kamelet definition files being included into the native image.
     * Note that this configuration option is only relevant when producing a native application.
     * </p>
     */
    @ConfigItem(defaultValue = "*")
    public List<String> identifiers;
}
