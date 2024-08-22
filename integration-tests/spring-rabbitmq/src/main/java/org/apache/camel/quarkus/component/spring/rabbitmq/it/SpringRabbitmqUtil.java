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
package org.apache.camel.quarkus.component.spring.rabbitmq.it;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Very simple serialization help to avoid dependencies for json serialization.
 */
public final class SpringRabbitmqUtil {

    private SpringRabbitmqUtil() {
    }

    public static String mapToString(Map<String, Object> map) {

        return map.keySet().stream()
                .map(key -> key + ":" + map.get(key))
                .collect(Collectors.joining(";"));
    }

    public static Map<String, Object> stringToMap(String s) {
        return Arrays.stream(s.split(";"))
                .map(kv -> kv.split(":"))
                .collect(Collectors.toMap(kv -> kv[0], kv -> kv[1]));
    }

    public static String listToString(List<String> list) {
        return list.stream().collect(Collectors.joining(";"));
    }

    public static List<Object> stringToList(String string) {
        return Arrays.asList(string.split(";"));
    }
}
