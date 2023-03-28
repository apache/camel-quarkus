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

package org.apache.camel.quarkus.component.cxf.soap.client.it;

import java.util.Map;
import java.util.UUID;

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
        final String BASIC_AUTH_USER = "tester";
        final String BASIC_AUTH_PASSWORD = UUID.randomUUID().toString();

        try {
            calculatorContainer = new GenericContainer<>("quay.io/l2x6/calculator-ws:1.1")
                    .withExposedPorts(WILDFLY_PORT)
                    .withEnv("BASIC_AUTH_USER", BASIC_AUTH_USER)
                    .withEnv("BASIC_AUTH_PASSWORD", BASIC_AUTH_PASSWORD)
                    .withLogConsumer(new Slf4jLogConsumer(log))
                    .waitingFor(Wait.forHttp("/calculator-ws/CalculatorService?wsdl"));

            calculatorContainer.start();

            return Map.of(
                    "camel-quarkus.it.calculator.baseUri",
                    "http://" + calculatorContainer.getHost() + ":" +
                            calculatorContainer.getMappedPort(WILDFLY_PORT),
                    "cq.cxf.it.calculator.auth.basic.user", BASIC_AUTH_USER,
                    "cq.cxf.it.calculator.auth.basic.password", BASIC_AUTH_PASSWORD);
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
