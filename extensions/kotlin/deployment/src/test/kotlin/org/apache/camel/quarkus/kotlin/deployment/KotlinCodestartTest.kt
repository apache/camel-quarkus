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
package org.apache.camel.quarkus.kotlin.deployment

import io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog
import io.quarkus.devtools.testing.codestarts.QuarkusCodestartTest
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.extension.RegisterExtension

class KotlinCodestartTest {

    @Test
    @Throws(Throwable::class)
    fun testContent() {
        Companion.codestartTest.checkGeneratedSource("org.acme.Routes")
    }

    @Disabled("https://github.com/quarkusio/quarkus/issues/34308")
    @Test
    @Throws(Throwable::class)
    fun buildAllProjects() {
        Companion.codestartTest.buildAllProjects()
    }

    companion object {
        @JvmField
        @RegisterExtension
        var codestartTest = QuarkusCodestartTest.builder()
            .languages(QuarkusCodestartCatalog.Language.KOTLIN)
            .setupStandaloneExtensionTest("org.apache.camel.quarkus:camel-quarkus-kotlin")
            .build()
    }
}
