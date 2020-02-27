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
package org.apache.camel.quarkus.kotlin

import org.apache.camel.RoutesBuilder
import org.apache.camel.builder.RouteBuilder

/**
 * Functional routes definition Kotlin DSL.
 *
 * Example:
 *
 * ```
 * fun myRoutes() = routes {
 *     from("timer:foo?period=1s")
 *         .log("\${body}")
 * }
 * ```
 *
 * In a nutshell, it makes it possible to configure routes with a lambda that acts as RouteBuilder.
 */
fun routes(block: RouteBuilder.() -> Unit) : RoutesBuilder {
    return object: RouteBuilder() {
        override fun configure() {
            this.block()
        }
    }
}
