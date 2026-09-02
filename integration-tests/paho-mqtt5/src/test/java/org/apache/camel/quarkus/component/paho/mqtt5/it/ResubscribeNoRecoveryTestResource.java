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
package org.apache.camel.quarkus.component.paho.mqtt5.it;

import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.quarkus.test.AvailablePortFinder;
import org.apache.camel.util.CollectionHelper;

public class ResubscribeNoRecoveryTestResource implements QuarkusTestResourceLifecycleManager {

    private FaultyMqtt5Broker broker;
    private int port;

    @Override
    public Map<String, String> start() {
        port = AvailablePortFinder.getNextAvailable();
        broker = new FaultyMqtt5Broker(port, Integer.MAX_VALUE);
        try {
            broker.start();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start FaultyMqtt5Broker", e);
        }
        return CollectionHelper.mapOf(
                "paho5.broker.tcp.url", "tcp://localhost:" + port);
    }

    @Override
    public void inject(TestInjector testInjector) {
        testInjector.injectIntoFields(broker,
                new TestInjector.AnnotatedAndMatchesType(InjectFaultyBroker.class, FaultyMqtt5Broker.class));
    }

    @Override
    public void stop() {
        if (broker != null) {
            broker.stop();
        }
    }
}
