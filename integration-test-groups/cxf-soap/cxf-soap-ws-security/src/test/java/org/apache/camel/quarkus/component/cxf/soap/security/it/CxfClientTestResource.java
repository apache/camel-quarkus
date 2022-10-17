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

package org.apache.camel.quarkus.component.cxf.soap.security.it;

import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

public class CxfClientTestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger log = LoggerFactory.getLogger(CxfClientTestResource.class);

    private static final int WILDFLY_PORT = 8080;
    private GenericContainer<?> calculatorContainer;

    @Override
    public Map<String, String> start() {

        final String user = "camel-quarkus-user-user";
        final String password = "secret-password";

        try {
            try {
                calculatorContainer = new GenericContainer<>("quay.io/l2x6/calculator-ws:1.0")
                        .withEnv("WSS_USER", user)
                        .withEnv("WSS_PASSWORD", password)
                        .withLogConsumer(new Slf4jLogConsumer(log))
                        .withExposedPorts(WILDFLY_PORT)
                        .waitingFor(Wait.forHttp("/calculator-ws/CalculatorService?wsdl"));

                calculatorContainer.start();

                return Map.of(
                        "camel-quarkus.it.calculator.baseUri",
                        "http://" + calculatorContainer.getHost() + ":" +
                                calculatorContainer.getMappedPort(WILDFLY_PORT),
                        "password-callback.username", user,
                        "password-callback.password", password);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        try {
            if (calculatorContainer != null) {
                calculatorContainer.stop();
            }
        } catch (Exception e) {
            // ignored
        }
    }
}
