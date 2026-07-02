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
package org.apache.camel.quarkus.component.a2a;

import java.util.Map;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.Endpoint;
import org.apache.camel.component.a2a.A2AComponent;
import org.apache.camel.component.a2a.A2AConstants;
import org.apache.camel.spi.annotations.Component;

@Recorder
public class A2aRecorder {

    public RuntimeValue<A2AComponent> createA2aComponent() {
        return new RuntimeValue<>(new CamelQuarkusA2AComponent());
    }

    @Component("a2a")
    static final class CamelQuarkusA2AComponent extends A2AComponent {
        @Override
        protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
            Object binding = parameters.get("protocolBinding");
            if (binding != null) {
                String value = binding.toString();
                if (A2AConstants.PROTOCOL_REST.equalsIgnoreCase(value)
                        || A2AConstants.PROTOCOL_REST_ALIAS.equalsIgnoreCase(value)) {
                    throw new IllegalArgumentException(
                            "The A2A REST (HTTP+JSON) protocol binding is not supported on Quarkus. "
                                    + "Use protocolBinding=JSONRPC instead.");
                }
            }
            return super.createEndpoint(uri, remaining, parameters);
        }
    }
}
