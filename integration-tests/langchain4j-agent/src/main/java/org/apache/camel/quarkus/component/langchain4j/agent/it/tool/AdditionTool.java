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
package org.apache.camel.quarkus.component.langchain4j.agent.it.tool;

import java.util.concurrent.atomic.AtomicBoolean;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

public class AdditionTool {
    private static final AtomicBoolean TOOL_WAS_INVOKED = new AtomicBoolean(false);

    @Tool("Adds two numbers")
    public Integer addNumbers(@P("First number") Integer a, @P("Second number") Integer b) {
        TOOL_WAS_INVOKED.set(true);
        return a + b;
    }

    public static boolean isToolWasInvoked() {
        return TOOL_WAS_INVOKED.get();
    }

    public static void reset() {
        TOOL_WAS_INVOKED.set(false);
    }
}
