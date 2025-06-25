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
package org.apache.camel.quarkus.component.console.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import org.apache.camel.quarkus.core.deployment.spi.CamelServiceDestination;
import org.apache.camel.quarkus.core.deployment.spi.CamelServicePatternBuildItem;

class ConsoleServicePatternProcessor {
    @BuildStep
    CamelServicePatternBuildItem devConsoleServicePattern() {
        // This BuildStep is separated from ConsoleProcessor as we need dev-console services to
        // always be discoverable, regardless of whether dev consoles are enabled or not.
        // This is to support 'internal' console usage such as the cli-connector & Camel JBang functionality.
        return new CamelServicePatternBuildItem(CamelServiceDestination.DISCOVERY, true,
                "META-INF/services/org/apache/camel/dev-console/*");
    }
}
