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
package org.apache.camel.quarkus.component.jolokia.it;

import java.util.concurrent.atomic.AtomicBoolean;

import org.jolokia.server.core.service.api.LogHandler;

public class CustomLogHandler implements LogHandler {
    public static AtomicBoolean MESSAGE_LOGGED = new AtomicBoolean(false);

    @Override
    public void debug(String message) {
        MESSAGE_LOGGED.set(true);
    }

    @Override
    public void info(String message) {
        MESSAGE_LOGGED.set(true);
    }

    @Override
    public void error(String message, Throwable t) {
        MESSAGE_LOGGED.set(true);
    }

    @Override
    public boolean isDebug() {
        return true;
    }

    public static Boolean isMessageLogged() {
        return MESSAGE_LOGGED.get();
    }
}
