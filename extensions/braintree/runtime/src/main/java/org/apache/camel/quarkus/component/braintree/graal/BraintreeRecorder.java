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
package org.apache.camel.quarkus.component.braintree.graal;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.component.braintree.BraintreeComponent;
import org.apache.camel.component.braintree.BraintreeConfiguration;

@Recorder
public class BraintreeRecorder {

    /**
     * Always disable the {@link org.apache.camel.component.braintree.internal.BraintreeLogHandler}.
     *
     * It's not desirable to configure this where an existing JUL - SLF4J bridge exists on the classpath.
     */
    public RuntimeValue<BraintreeComponent> configureBraintreeComponent() {
        BraintreeComponent component = new BraintreeComponent();
        BraintreeConfiguration configuration = new BraintreeConfiguration();
        configuration.setLogHandlerEnabled(false);
        component.setConfiguration(configuration);
        return new RuntimeValue<>(component);
    }
}
