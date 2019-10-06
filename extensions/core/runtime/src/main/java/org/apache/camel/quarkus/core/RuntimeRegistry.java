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

import java.util.LinkedHashSet;
import java.util.Set;

import io.quarkus.runtime.RuntimeValue;
import org.apache.camel.spi.BeanRepository;
import org.apache.camel.support.DefaultRegistry;

public class RuntimeRegistry extends DefaultRegistry {
    public RuntimeRegistry() {
        super(new RuntimeBeanRepository());
    }

    @Override
    public Object unwrap(Object value) {
        return (value instanceof RuntimeValue)
            ? ((RuntimeValue)value).getValue()
            : value;
    }

    //
    // DefaultRegistry does not merge results from the repositories
    // and fallback registry so in case beans are bound to the local
    // registry only but any of the configured repositories returns
    // a non null answer, then the local values are not taken into
    // account for the final answer.
    //
    // TODO: fix upstream and remove this method
    //
    @Override
    public <T> Set<T> findByType(Class<T> type) {
        final Set<T> answer = new LinkedHashSet<>();

        if (repositories != null) {
            for (BeanRepository r : repositories) {
                Set<T> instances = r.findByType(type);
                if (instances != null && !instances.isEmpty()) {
                    answer.addAll(instances);
                }
            }
        }

        Set<T> instances = fallbackRegistry.findByType(type);
        if (instances != null && !instances.isEmpty()) {
            for (T instance: instances) {
                answer.add((T)unwrap(instance));
            }
        }

        return answer;
    }
}
