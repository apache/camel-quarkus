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
package org.apache.camel.quarkus.k;

import org.apache.camel.quarkus.k.core.SourceDefinition;
import org.apache.camel.quarkus.k.support.Sources;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

public class SourceTest {
    @Test
    public void testResourceWithoutScheme() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(
                () -> Sources.fromURI("routes.js"));
    }

    @Test
    public void testResourceWithIllegalScheme() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(
                () -> Sources.fromURI("http:routes.js"));
    }

    @Test
    public void testUnsupportedLanguage() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(
                () -> Sources.fromURI("  test"));
    }

    @Test
    public void sourceCanBeContructedFromLocation() {
        SourceDefinition definition = new SourceDefinition();
        definition.setLocation("classpath:MyRoutes.java");

        assertThat(Sources.fromDefinition(definition))
                .hasFieldOrPropertyWithValue("name", "MyRoutes")
                .hasFieldOrPropertyWithValue("language", "java");
    }
}
