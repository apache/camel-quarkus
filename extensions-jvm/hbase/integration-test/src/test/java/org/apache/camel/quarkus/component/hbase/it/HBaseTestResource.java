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
package org.apache.camel.quarkus.component.hbase.it;

import java.util.Collections;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.jboss.logging.Logger;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class HBaseTestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOG = Logger.getLogger(HBaseTestResource.class);
    // must be the same as in the config of camel component
    static final Integer CLIENT_PORT = 2181;

    private GenericContainer<?> container;

    @Override
    public Map<String, String> start() {

        try {
            //there is only one tag for this docker image -  latest. See https://hub.docker.com/r/dajobe/hbase/tags
            //Hbase is using zookeeper. Hbase client gets location of hbase master from zookeeper, which means that
            //location uses internal hostnames from the docker.  Network mode `host` is the only way how to avoid
            //manipulation with the hosts configuration at the test server.
            container = new GenericContainer<>("dajobe/hbase:latest")
                    .withNetworkMode("host")
                    .withLogConsumer(frame -> System.out.print(frame.getUtf8String()))
                    .waitingFor(
                            Wait.forLogMessage(".*Finished refreshing block distribution cache for 2 regions\\n", 1));
            container.start();

            return Collections.emptyMap();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        try {
            if (container != null) {
                container.stop();
            }
        } catch (Exception e) {
            // Ignored
        }
    }

}
