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
package org.apache.camel.quarkus.component.box.it;

import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import io.quarkus.bootstrap.util.IoUtils;
import io.quarkus.test.common.NativeImageStartedNotifier;
import org.awaitility.Awaitility;

/**
 * TODO: Investigate why the native app takes so long to start and eventually remove this
 * https://github.com/apache/camel-quarkus/issues/2830
 */
public class CamelBoxNativeApplicationStartedNotifier implements NativeImageStartedNotifier {

    @Override
    public boolean isNativeImageStarted() {
        Awaitility.await().pollDelay(1, TimeUnit.SECONDS).timeout(30, TimeUnit.SECONDS).until(() -> {
            String log = IoUtils.readFile(Paths.get("target/quarkus.log"));
            return log.contains("Installed features");
        });
        return true;
    }
}
