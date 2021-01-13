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
package org.apache.camel.quarkus.test.mock.backend;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

public class MockBackendUtils {

    private static boolean startMockBackend = ConfigProvider.getConfig()
            .getOptionalValue("camel.quarkus.start-mock-backend", Boolean.class).orElse(Boolean.TRUE);

    private static final Logger LOG = Logger.getLogger(MockBackendUtils.class);

    public static void logBackendUsed() {
        if (startMockBackend) {
            logMockBackendUsed();
        } else {
            logRealBackendUsed();
        }
    }

    public static void logRealBackendUsed() {
        LOG.infof("Real backend will be used");
    }

    public static void logMockBackendUsed() {
        LOG.infof("Mock backend will be used");
    }

    public static boolean startMockBackend() {
        return startMockBackend(false);
    }

    public static boolean startMockBackend(boolean printLogMessage) {
        if (printLogMessage) {
            logBackendUsed();
        }
        return startMockBackend;
    }
}
