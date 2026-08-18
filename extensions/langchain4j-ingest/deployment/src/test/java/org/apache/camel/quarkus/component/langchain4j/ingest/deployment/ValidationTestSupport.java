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
package org.apache.camel.quarkus.component.langchain4j.ingest.deployment;

import java.util.Arrays;

/**
 * The validation messages are the extension's UX: every test here asserts the words a user
 * actually reads, so a rewording is a conscious act rather than an accident.
 */
final class ValidationTestSupport {

    private ValidationTestSupport() {
    }

    /** Asserts that some cause in the chain carries a message containing every fragment. */
    static void assertFailure(Throwable throwable, String... fragments) {
        for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
            String message = cause.getMessage();
            if (message != null && Arrays.stream(fragments).allMatch(message::contains)) {
                return;
            }
            if (cause.getCause() == cause) {
                break;
            }
        }
        throw new AssertionError(
                "No cause message contains all of " + Arrays.toString(fragments) + "; got: " + throwable, throwable);
    }
}
