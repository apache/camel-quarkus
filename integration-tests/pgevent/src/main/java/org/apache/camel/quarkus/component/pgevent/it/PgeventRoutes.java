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

import javax.inject.Named;
import javax.ws.rs.Produces;

import com.impossibl.postgres.jdbc.PGDataSource;
import org.apache.camel.builder.RouteBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

public class PgeventRoutes extends RouteBuilder {
    @ConfigProperty(name = "database.host")
    String host;
    @ConfigProperty(name = "database.port")
    Integer port;
    @ConfigProperty(name = "database.name")
    String databaseName;
    @ConfigProperty(name = "database.user")
    String user;
    @ConfigProperty(name = "database.password")
    String password;

    @Override
    public void configure() throws Exception {
        // producer for simple pub-sub
        from("direct:pgevent-pub")
                .to("pgevent://{{database.host}}:{{database.port}}/{{database.name}}/testchannel?user={{database.user}}&pass={{database.password}}");

        //consumer for simple pub-sub
        from("pgevent://{{database.host}}:{{database.port}}/{{database.name}}/testchannel?user={{database.user}}&pass={{database.password}}")
                .log("Message got ${body}")
                .bean(MyBean.class);

        // producer with datasource
        from("direct:pgevent-datasource")
                .to("pgevent:///postgres/testchannel?datasource=#pgDataSource");

        // consumer with datasource
        from("pgevent:///postgres/testchannel?datasource=#pgDataSource")
                .log("Message got ${body}")
                .bean(MyBean.class);
    }

    @Produces
    @Named("pgDataSource")
    public PGDataSource loadDataSource() throws Exception {
        PGDataSource dataSource = new PGDataSource();
        dataSource.setHost(host);
        dataSource.setPort(port);
        dataSource.setDatabaseName(databaseName);
        dataSource.setUser(user);
        dataSource.setPassword(password);
        return dataSource;
    }
}
