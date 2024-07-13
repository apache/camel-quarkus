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
package org.apache.camel.quarkus.core.deployment;

import java.util.function.BooleanSupplier;
import java.util.stream.StreamSupport;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.AllowJNDIBuildItem;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 * Build steps relating to camel debugging support. This is primarily required due to camel-main
 * having the capability to enable debugging features that live in the Camel core such as the
 * DebuggerJmxConnectorService
 */
@BuildSteps(onlyIf = CamelDebugProcessor.CamelDebugConfigurationPresent.class)
public class CamelDebugProcessor {
    @BuildStep
    AllowJNDIBuildItem allowJNDI() {
        return new AllowJNDIBuildItem();
    }

    static final class CamelDebugConfigurationPresent implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            return StreamSupport.stream(ConfigProvider.getConfig().getPropertyNames().spliterator(), false)
                    .anyMatch(key -> key.startsWith("camel.debug"));
        }
    }
}
