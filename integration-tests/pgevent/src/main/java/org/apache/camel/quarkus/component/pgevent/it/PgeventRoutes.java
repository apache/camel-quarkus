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
package org.apache.camel.quarkus.component.pgevent.it;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class PgeventRoutes extends RouteBuilder {

    public static final String MOCK_ENDPOINT_CLASSIC_CONF = "mock:classic";
    public static final String MOCK_ENDPOINT_AGROALDATASOURCE = "mock:datasource";

    @ConfigProperty(name = "database.host")
    String host;
    @ConfigProperty(name = "database.port")
    Integer port;
    @ConfigProperty(name = "database.name")
    String databaseName;
    @ConfigProperty(name = "quarkus.datasource.pgDatasource.username")
    String user;
    @ConfigProperty(name = "quarkus.datasource.pgDatasource.password")
    String password;

    @Override
    public void configure() throws Exception {
        // producer for simple pub-sub
        from("direct:pgevent-pub")
                .to(String.format("pgevent://%s:%s/%s/testchannel?user=%s&pass=%s", host, port, databaseName, user, password));

        //consumer for simple pub-sub
        from(String.format("pgevent://%s:%s/%s/testchannel?user=%s&pass=%s", host, port, databaseName, user, password))
                .to(MOCK_ENDPOINT_CLASSIC_CONF);

        // producer with datasource
        from("direct:pgevent-datasource")
                .to("pgevent:///postgres/testchannel?datasource=#pgDatasource");

        // consumer with datasource
        from("pgevent:///postgres/testchannel?datasource=#pgDatasource")
                .to(MOCK_ENDPOINT_AGROALDATASOURCE);
    }

}
