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
<<<<<<<< HEAD:integration-tests/main-yaml/src/main/java/org/apache/camel/quarkus/main/ErrorBean.java
package org.apache.camel.quarkus.main;
========
package org.apache.camel.quarkus.component.dataformat.json.johnzon;
>>>>>>>> de57a77464 (Split json dataformats to different modules):integration-test-groups/dataformats-json/json-johnzon/src/test/java/org/apache/camel/quarkus/component/dataformat/json/johnzon/JohnzonJsonIT.java

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class ErrorBean {
    public void throwException() throws CustomException {
        throw new CustomException();
    }
}
