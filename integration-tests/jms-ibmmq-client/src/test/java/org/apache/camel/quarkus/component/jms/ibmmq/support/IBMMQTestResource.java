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
package org.apache.camel.quarkus.component.jms.ibmmq.support;

import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.eclipse.microprofile.config.ConfigProvider;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

public class IBMMQTestResource implements QuarkusTestResourceLifecycleManager {
    private static final String IMAGE_NAME = ConfigProvider.getConfig().getValue("ibm-mq.container.image", String.class);
    private static final int PORT = 1414;
    private static final String QUEUE_MANAGER_NAME = "QM1";
    private static final String USER = "app";
    private static final String PASSWORD = "passw0rd";
    private static final String MESSAGING_CHANNEL = "DEV.APP.SVRCONN";
    private static final String MQSC_COMMAND_FILE_NAME = "99-auth.mqsc";
    private static final String MQSC_FILE_CONTAINER_PATH = "/etc/mqm/" + MQSC_COMMAND_FILE_NAME;

    private GenericContainer<?> container;
    private IBMMQDestinations destinations;

    @Override
    public Map<String, String> start() {
        container = new GenericContainer<>(DockerImageName.parse(IMAGE_NAME))
                .withExposedPorts(PORT)
                .withEnv(Map.of(
                        "LICENSE", System.getProperty("ibm.mq.container.license"),
                        "MQ_QMGR_NAME", QUEUE_MANAGER_NAME))
                .withCopyToContainer(Transferable.of(PASSWORD), "/run/secrets/mqAdminPassword")
                .withCopyToContainer(Transferable.of(PASSWORD), "/run/secrets/mqAppPassword")
                .withCopyToContainer(Transferable.of(mqscConfig()), MQSC_FILE_CONTAINER_PATH)
                // AMQ5806I is a message code for queue manager start
                .waitingFor(Wait.forLogMessage(".*AMQ5806I.*", 1));
        container.start();

        destinations = new IBMMQDestinations(container.getHost(), container.getMappedPort(PORT), QUEUE_MANAGER_NAME);

        return Map.of(
                "ibm.mq.host", container.getHost(),
                "ibm.mq.port", container.getMappedPort(PORT).toString(),
                "ibm.mq.user", USER,
                "ibm.mq.password", PASSWORD,
                "ibm.mq.queueManagerName", QUEUE_MANAGER_NAME,
                "ibm.mq.channel", MESSAGING_CHANNEL);
    }

    @Override
    public void stop() {
        if (container != null) {
            container.stop();
        }
    }

    @Override
    public void inject(TestInjector testInjector) {
        testInjector.injectIntoFields(destinations, new TestInjector.MatchesType(IBMMQDestinations.class));
    }

    /**
     * By default the user does have access just to predefined queues, this will add permissions to access
     * all standard queues + topics and a special system queue.
     *
     * @return mqsc config string
     */
    private String mqscConfig() {
        return "SET AUTHREC PROFILE('*') PRINCIPAL('" + USER + "') OBJTYPE(TOPIC) AUTHADD(ALL)\n"
                + "SET AUTHREC PROFILE('*') PRINCIPAL('" + USER + "') OBJTYPE(QUEUE) AUTHADD(ALL)\n"
                + "SET AUTHREC PROFILE('SYSTEM.DEFAULT.MODEL.QUEUE') OBJTYPE(QUEUE) PRINCIPAL('" + USER + "') AUTHADD(ALL)";
    }
}
