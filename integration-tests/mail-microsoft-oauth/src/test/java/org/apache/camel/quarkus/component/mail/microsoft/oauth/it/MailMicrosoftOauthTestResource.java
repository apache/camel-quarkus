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
package org.apache.camel.quarkus.component.mail.microsoft.oauth.it;

import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class MailMicrosoftOauthTestResource implements QuarkusTestResourceLifecycleManager {

    @Override
    public Map<String, String> start() {
        // we must provide test subject as property, because having only static field means that in Native tests, we would use different unique value (as app runs natively and tests in JVM, thus double invocation)
        return Map.of("test.mail.subject", "CamelQuarkus" + System.currentTimeMillis());
    }

    @Override
    public void stop() {
        // Noop
    }
}
