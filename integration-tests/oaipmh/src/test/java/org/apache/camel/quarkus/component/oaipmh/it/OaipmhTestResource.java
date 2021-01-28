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
package org.apache.camel.quarkus.component.oaipmh.it;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.quarkus.test.AvailablePortFinder;

public class OaipmhTestResource implements QuarkusTestResourceLifecycleManager {

    private OaipmhTestServer test1Server, test2Server, test3Server, test4Server;

    @Override
    public Map<String, String> start() {
        Map<String, String> systemProperties = new HashMap<>();
        this.test1Server = initOaipmhTestServer("test1", systemProperties, false);
        this.test2Server = initOaipmhTestServer("test2", systemProperties, false);
        this.test3Server = initOaipmhTestServer("test3", systemProperties, false);
        this.test4Server = initOaipmhTestServer("test4", systemProperties, true);
        return systemProperties;
    }

    private OaipmhTestServer initOaipmhTestServer(String context, Map<String, String> properties, boolean useHttps) {
        int availablePort = AvailablePortFinder.getNextAvailable();
        OaipmhTestServer server = new OaipmhTestServer(context, availablePort, useHttps);
        server.startServer();
        properties.put("cq-oaipmh-its." + context + ".server.authority", "localhost:" + availablePort);
        return server;
    }

    @Override
    public void stop() {
        if (test1Server != null) {
            test1Server.stopServer();
        }
        if (test2Server != null) {
            test2Server.stopServer();
        }
        if (test3Server != null) {
            test3Server.stopServer();
        }
        if (test4Server != null) {
            test4Server.stopServer();
        }
    }
}
