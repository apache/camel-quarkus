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
package org.apache.camel.quarkus.component.cassandraql.it;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import jakarta.inject.Named;
import org.apache.camel.component.cassandra.ResultSetConversionStrategy;
import org.eclipse.microprofile.config.inject.ConfigProperty;

public class CassandraqlProducers {

    @Named
    public CqlSession customCqlSession(
            @ConfigProperty(name = "quarkus.cassandra.auth.username") String username,
            @ConfigProperty(name = "quarkus.cassandra.auth.password") String password,
            @ConfigProperty(name = "quarkus.cassandra.contact-points") String dbUrl) {
        String[] urlParts = dbUrl.split(":");
        CqlSessionBuilder sessionBuilder = CqlSession.builder();
        sessionBuilder.addContactPoint(new InetSocketAddress(urlParts[0], Integer.parseInt(urlParts[1])));
        sessionBuilder.withLocalDatacenter("datacenter1");
        sessionBuilder.withKeyspace(CassandraqlRoutes.KEYSPACE);
        sessionBuilder.withAuthCredentials(username, password);
        return sessionBuilder.build();
    }

    @Named
    public ResultSetConversionStrategy customResultSetConversionStrategy() {
        return resultSet -> {
            List<Employee> employees = new ArrayList<>();
            resultSet.forEach(row -> {
                String name = row.getString("name");
                String address = row.getString("address");
                int id = row.getInt("id");

                Employee employee = new Employee(id, name + " modified", address);
                employees.add(employee);
            });
            return employees;
        };
    }
}
