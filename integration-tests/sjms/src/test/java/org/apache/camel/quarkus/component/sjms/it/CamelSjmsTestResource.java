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
package org.apache.camel.quarkus.component.sjms.it;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.camel.quarkus.core.CamelMain;
import org.apache.camel.quarkus.test.AvailablePortFinder;
import org.apache.commons.io.FileUtils;

public class CamelSjmsTestResource implements QuarkusTestResourceLifecycleManager {
    private CamelMain main;
    private EmbeddedActiveMQ embedded;

    @Override
    public void inject(Object testInstance) {
        if (testInstance instanceof CamelSjmsTest) {
            this.main = ((CamelSjmsTest) testInstance).main;
        }
    }

    @Override
    public Map<String, String> start() {
        try {
            final File dataDirectory = Paths.get("./target/artemis").toFile();
            FileUtils.deleteDirectory(dataDirectory);

            final int port = AvailablePortFinder.getNextAvailable();
            final String url = String.format("tcp://127.0.0.1:%d", port);

            ConfigurationImpl cfg = new ConfigurationImpl();
            cfg.addAcceptorConfiguration("activemq", url);
            cfg.setSecurityEnabled(false);
            cfg.setBrokerInstance(dataDirectory);

            embedded = new EmbeddedActiveMQ();
            embedded.setConfiguration(cfg);
            embedded.start();

            return Collections.singletonMap("quarkus.artemis.url", url);
        } catch (Exception e) {
            throw new RuntimeException("Could not start embedded ActiveMQ server", e);
        }
    }

    @Override
    public void stop() {
        try {
            if (main != null) {
                main.stop();
            }
        } catch (Exception e) {
            // ignored
        }
        try {
            if (embedded != null) {
                embedded.stop();
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not stop embedded ActiveMQ server", e);
        }
    }
}
