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
const consumer = Java.type("org.apache.camel.quarkus.dsl.js.runtime.JavaScriptDslConsumer");
const c = new consumer("msg", `msg.setBody('From consumer')`);
const processor = Java.type("org.apache.camel.quarkus.dsl.js.runtime.JavaScriptDslProcessor");
const p = new processor(`exchange.getMessage().setBody('From processor')`);

from('direct:routes-with-processors-consumer')
    .id("routes-with-processors-consumer")
    .process().message(c)
    .filter().simple("${body} == 'From consumer'")
    .setBody().constant("true");

from('direct:routes-with-processors-processor')
    .id("routes-with-processors-processor")
    .process(p)
    .filter().simple("${body} == 'From processor'")
    .setBody().constant("true");
