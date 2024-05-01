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
package org.apache.camel.quarkus.component.bean;

import org.apache.camel.PropertyInject;

public class PropertyInjectedFieldBean {
    @PropertyInject("my.injected.property.a")
    private String injectedPropertyA;

    @PropertyInject(value = "my.injected.property.b", defaultValue = "Test @PropertyInject default")
    private String injectedPropertyB;

    @PropertyInject(value = "Hello {{my.injected.property.a}} Placeholder")
    private String injectedPropertyC;

    public String getInjectedPropertyA() {
        return injectedPropertyA;
    }

    public String getInjectedPropertyB() {
        return injectedPropertyB;
    }

    public String getInjectedPropertyC() {
        return injectedPropertyC;
    }
}
