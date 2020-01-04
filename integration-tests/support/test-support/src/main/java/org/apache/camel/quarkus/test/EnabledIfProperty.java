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
package org.apache.camel.quarkus.test;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * {@code @EnabledIfProperty} is used to signal that the annotated test
 * class or test method is only <em>enabled</em> if the value of the specified
 * {@linkplain #named property} obtained through MP Config matches the specified
 * {@linkplain #matches regular expression}.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ExtendWith(EnabledIfPropertyCondition.class)
public @interface EnabledIfProperty {
    /**
     * The name of the property to retrieve.
     *
     * @return the property name; never <em>blank</em>
     * @see    org.eclipse.microprofile.config.Config#getValue(String, Class)
     */
    String named();

    /**
     * A regular expression that will be used to match against the retrieved
     * value of the {@link #named} property.
     *
     * @return the regular expression; never <em>blank</em>
     * @see    String#matches(String)
     * @see    java.util.regex.Pattern
     */
    String matches();

}
