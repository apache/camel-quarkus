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
package org.apache.camel.quarkus.k.runtime;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PropertiesFunctionsConfigurerTest {

    @Test
    public void testFunction() throws Exception {
        try (CamelContext context = new DefaultCamelContext()) {
            context.getPropertiesComponent().addInitialProperty("my.property", "{{secret:my-secret/my-property}}");

            assertThat(context.resolvePropertyPlaceholders("{{secret:my-secret/my-property}}"))
                    .isEqualTo("my-secret-property");
            assertThat(context.resolvePropertyPlaceholders("{{secret:none/my-property:my-default-secret}}"))
                    .isEqualTo("my-default-secret");

            assertThatThrownBy(() -> context.resolvePropertyPlaceholders("{{secret:none/my-property}}"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("returned null value which is not allowed, from input");

            assertThat(context.resolvePropertyPlaceholders("{{configmap:my-cm/my-property}}"))
                    .isEqualTo("my-cm-property");
            assertThat(context.resolvePropertyPlaceholders("{{configmap:my-cm/my-property:my-default-cm}}"))
                    .isEqualTo("my-default-cm");

            assertThatThrownBy(() -> context.resolvePropertyPlaceholders("{{configmap:none/my-property}}"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("returned null value which is not allowed, from input");

            assertThat(context.resolvePropertyPlaceholders("{{my.property}}")).isEqualTo("my-secret-property");
            assertThat(context.resolvePropertyPlaceholders("{{my-app-property}}")).isEqualTo("my-app-value");
        }
    }
}
