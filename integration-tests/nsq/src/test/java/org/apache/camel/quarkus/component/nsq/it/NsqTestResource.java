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

package org.apache.camel.quarkus.component.nsq.it;

import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.TestcontainersConfiguration;

import static org.apache.camel.quarkus.component.nsq.it.NsqLogger.log;
import static org.apache.camel.quarkus.component.nsq.it.NsqRoute.CONSUMER_HOST_CFG_KEY;
import static org.apache.camel.quarkus.component.nsq.it.NsqRoute.CONSUMER_PORT_CFG_KEY;
import static org.apache.camel.quarkus.component.nsq.it.NsqRoute.PRODUCER_HOST_CFG_KEY;
import static org.apache.camel.quarkus.component.nsq.it.NsqRoute.PRODUCER_PORT_CFG_KEY;
import static org.apache.camel.util.CollectionHelper.mapOf;

public class NsqTestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOG = LoggerFactory.getLogger(NsqTestResource.class);

    public static final String CONTAINER_NSQ_IMAGE = "nsqio/nsq:v1.2.0";

    public static final String CONTAINER_NSQLOOKUPD_NAME = "nsqlookupd";
    public static final int CONTAINER_NSQLOOKUPD_TCP_PORT = 4160;
    public static final int CONTAINER_NSQLOOKUPD_HTTP_PORT = 4161;

    public static final String CONTAINER_NSQD_NAME = "nsqd";
    public static final int CONTAINER_NSQD_TCP_PORT = 4150;

    private GenericContainer nsqDaemonContainer, nsqLookupDaemonContainer;

    @Override
    public Map<String, String> start() {
        log(LOG, "%s", TestcontainersConfiguration.getInstance().toString());

        Network network = Network.newNetwork();

        nsqLookupDaemonContainer = new FixedHostPortGenericContainer(CONTAINER_NSQ_IMAGE)
                .withFixedExposedPort(CONTAINER_NSQLOOKUPD_HTTP_PORT, CONTAINER_NSQLOOKUPD_HTTP_PORT)
                .withNetworkAliases(CONTAINER_NSQLOOKUPD_NAME)
                .withCommand("/nsqlookupd")
                .withNetwork(network)
                .withLogConsumer(new Slf4jLogConsumer(LOG))
                .waitingFor(Wait.forLogMessage(".*TCP: listening on.*", 1));
        nsqLookupDaemonContainer.start();

        String nsqdCmdFormat = "/nsqd --broadcast-address=%s --lookupd-tcp-address=%s:%s";
        String nsqdCmd = String.format(nsqdCmdFormat, "localhost", CONTAINER_NSQLOOKUPD_NAME, CONTAINER_NSQLOOKUPD_TCP_PORT);
        nsqDaemonContainer = new FixedHostPortGenericContainer(CONTAINER_NSQ_IMAGE)
                .withFixedExposedPort(CONTAINER_NSQD_TCP_PORT, CONTAINER_NSQD_TCP_PORT)
                .withNetworkAliases(CONTAINER_NSQD_NAME)
                .withCommand(nsqdCmd)
                .withNetwork(network)
                .withLogConsumer(new Slf4jLogConsumer(LOG))
                .waitingFor(Wait.forLogMessage(".*TCP: listening on.*", 1));
        nsqDaemonContainer.start();

        String nsqConsumerHost = nsqLookupDaemonContainer.getContainerIpAddress();
        Integer nsqConsumerPort = nsqLookupDaemonContainer.getMappedPort(CONTAINER_NSQLOOKUPD_HTTP_PORT);

        String nsqProducerHost = nsqDaemonContainer.getContainerIpAddress();
        Integer nsqProducerPort = nsqDaemonContainer.getMappedPort(CONTAINER_NSQD_TCP_PORT);

        return mapOf(CONSUMER_HOST_CFG_KEY, nsqConsumerHost, CONSUMER_PORT_CFG_KEY, "" + nsqConsumerPort,
                PRODUCER_HOST_CFG_KEY, nsqProducerHost, PRODUCER_PORT_CFG_KEY, "" + nsqProducerPort);
    }

    @Override
    public void stop() {
        log(LOG, "Logs for nsqLookupContainer: %s", nsqLookupDaemonContainer.getLogs());
        log(LOG, "Logs for nsqContainer: %s", nsqDaemonContainer.getLogs());

        try {
            if (nsqLookupDaemonContainer != null) {
                nsqLookupDaemonContainer.stop();
            }
        } catch (Exception ex) {
            log(LOG, "An issue occured while stopping nsqLookupContainer %s:", ex.getMessage());
        }

        try {
            if (nsqDaemonContainer != null) {
                nsqDaemonContainer.stop();
            }
        } catch (Exception ex) {
            log(LOG, "An issue occured while stopping nsqContainer %s:", ex.getMessage());
        }
    }
}
