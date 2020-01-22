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

import java.util.Map;
import java.util.Objects;

import org.assertj.core.api.Condition;

public final class CamelTestConditions {
    private CamelTestConditions() {
    }

    public static Condition<Map.Entry<String, String>> entry(String key, String value) {
        return new Condition<Map.Entry<String, String>>() {
            @Override
            public boolean matches(Map.Entry<String, String> entry) {
                return Objects.equals(entry.getKey(), key) && Objects.equals(entry.getValue(), value);
            }
        };
    }

    public static Condition<String> startsWith(String prefix) {
        return new Condition<String>("Starts with " + prefix) {
            @Override
            public boolean matches(String value) {
                return value.startsWith(prefix);
            }
        };
    }

    public static Condition<String> doesNotStartWith(String prefix) {
        return new Condition<String>("Does not start with " + prefix) {
            @Override
            public boolean matches(String value) {
                return !value.startsWith(prefix);
            }
        };
    }
}
