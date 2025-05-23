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
package org.apache.camel.quarkus.component.pinecone.it;

import java.util.List;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.util.CollectionHelper;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.pinecone.PineconeLocalContainer;

public class PineconeTestResource implements QuarkusTestResourceLifecycleManager {

    private static final Logger log = LoggerFactory.getLogger(PineconeTestResource.class);
    private static final String PINECONE_IMAGE = ConfigProvider.getConfig().getValue("pinecone.container.image", String.class);

    private PineconeLocalContainer container;

    @Override
    public Map<String, String> start() {
        try {
            container = new PineconeLocalContainer(PINECONE_IMAGE)
                    .withLogConsumer(new Slf4jLogConsumer(log))
                    .waitingFor(Wait.forListeningPort());

            // port 5081 for the index
            container.setPortBindings(List.of("5080:5080", "5081:5081"));

            container.start();

            return CollectionHelper.mapOf("pinecone.emulator.endpoint", container.getEndpoint());

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
            // ignored
        }
    }
}
