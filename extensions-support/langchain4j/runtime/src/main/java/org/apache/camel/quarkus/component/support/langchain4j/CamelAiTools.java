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

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.enterprise.util.Nonbinding;
import jakarta.interceptor.InterceptorBinding;

/**
 * Filters the Camel AI tools exposed to a {@code @RegisterAiService} by tag. When placed on a
 * {@code @RegisterAiService} interface, only {@code ai-tool:} routes whose {@code tags} parameter includes the
 * specified value (plus any routes in the default pool) are provided to the AI service.
 *
 * <pre>
 * &#64;RegisterAiService
 * &#64;CamelAiTools("support")
 * public interface SupportAgent {
 *     String chat(@UserMessage String message);
 * }
 * </pre>
 */
@InterceptorBinding
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface CamelAiTools {
    @Nonbinding
    String value() default "";
}
