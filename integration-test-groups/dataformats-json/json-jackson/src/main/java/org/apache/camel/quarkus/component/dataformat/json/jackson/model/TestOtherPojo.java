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
<<<<<<<< HEAD:integration-tests/main-yaml/src/main/java/org/apache/camel/quarkus/main/GreetingBean.java
package org.apache.camel.quarkus.main;
========
package org.apache.camel.quarkus.component.dataformat.json.jackson.model;
>>>>>>>> de57a77464 (Split json dataformats to different modules):integration-test-groups/dataformats-json/json-jackson/src/main/java/org/apache/camel/quarkus/component/dataformat/json/jackson/model/TestOtherPojo.java

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
<<<<<<<< HEAD:integration-tests/main-yaml/src/main/java/org/apache/camel/quarkus/main/GreetingBean.java
public class GreetingBean {
    String greeting;

    public void setGreeting(String greeting) {
        this.greeting = greeting;
    }

    public String greet() {
        return greeting;
========
public class TestOtherPojo {

    private String name;
    private String country;

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
>>>>>>>> de57a77464 (Split json dataformats to different modules):integration-test-groups/dataformats-json/json-jackson/src/main/java/org/apache/camel/quarkus/component/dataformat/json/jackson/model/TestOtherPojo.java
    }

}
