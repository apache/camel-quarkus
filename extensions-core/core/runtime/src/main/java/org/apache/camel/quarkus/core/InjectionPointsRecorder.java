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
package org.apache.camel.quarkus.core;

import java.util.function.Supplier;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.CamelContext;
import org.apache.camel.Component;

@Recorder
public class InjectionPointsRecorder {
    public Supplier<? extends Component> componentSupplier(String componentName, String componentType) {
        return new Supplier<Component>() {
            @Override
            public Component get() {
                // We can't inject the CamelContext from the BuildStep as it will create a
                // dependency cycle as the BuildStep that creates the CamelContext requires
                // BeanManger instance. As this is a fairly trivial job, we can safely keep
                // the context lookup-at runtime.
                final CamelContext camelContext = Arc.container().instance(CamelContext.class).get();
                if (camelContext == null) {
                    throw new IllegalStateException("No CamelContext found");
                }

                return camelContext.getComponent(
                        componentName,
                        camelContext.getClassResolver().resolveClass(componentType, Component.class));
            }
        };
    }
}
