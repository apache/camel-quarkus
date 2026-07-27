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
package org.apache.camel.quarkus.component.support.langchain4j;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * CDI interceptor that sets the current Camel AI tool tag on a ThreadLocal before an AI service method executes.
 * This allows {@link CamelAiToolProvider#provideTools} to filter tools by the tag associated with the calling AI
 * service,
 * enabling multiple {@code @RegisterAiService} interfaces with different {@code @CamelAiTools} tags in the same
 * application.
 */
@Interceptor
@CamelAiTools
@Priority(Interceptor.Priority.LIBRARY_BEFORE + 100)
public class CamelAiToolsInterceptor {

    @AroundInvoke
    Object aroundInvoke(InvocationContext ctx) throws Exception {
        Class<?> targetClass = ctx.getTarget().getClass();
        String tag = resolveTag(targetClass);
        String previous = CamelAiToolProvider.getCurrentTag();
        if (tag != null) {
            CamelAiToolProvider.setCurrentTag(tag);
        }
        try {
            return ctx.proceed();
        } finally {
            if (previous != null) {
                CamelAiToolProvider.setCurrentTag(previous);
            } else {
                CamelAiToolProvider.clearCurrentTag();
            }
        }
    }

    private String resolveTag(Class<?> targetClass) {
        // Walk the full class hierarchy: ArC creates $$QuarkusImpl_Subclass extending $$QuarkusImpl,
        // and the @CamelAiTools interface is declared on $$QuarkusImpl (not the subclass), so we
        // must check superclasses and all their interfaces.
        for (Class<?> clazz = targetClass; clazz != null && clazz != Object.class; clazz = clazz.getSuperclass()) {
            String tag = CamelAiToolProvider.TAG_MAP.get(clazz.getName());
            if (tag != null) {
                return tag;
            }
            for (Class<?> iface : clazz.getInterfaces()) {
                tag = CamelAiToolProvider.TAG_MAP.get(iface.getName());
                if (tag != null) {
                    return tag;
                }
            }
        }
        return null;
    }
}
