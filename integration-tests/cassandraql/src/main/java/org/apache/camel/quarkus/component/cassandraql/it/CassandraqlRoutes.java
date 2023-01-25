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

import com.datastax.oss.quarkus.runtime.api.session.QuarkusCqlSession;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.AggregationStrategy;
import org.apache.camel.BindToRegistry;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.processor.aggregate.cassandra.CassandraAggregationRepository;
import org.apache.camel.processor.aggregate.cassandra.NamedCassandraAggregationRepository;
import org.apache.camel.processor.idempotent.cassandra.NamedCassandraIdempotentRepository;
import org.apache.camel.spi.AggregationRepository;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class CassandraqlRoutes extends RouteBuilder {
    public static final String KEYSPACE = "test";

    @ConfigProperty(name = "quarkus.cassandra.contact-points")
    String dbUrl;

    @ConfigProperty(name = "quarkus.cassandra.auth.username")
    String userName;

    @ConfigProperty(name = "quarkus.cassandra.auth.password")
    String password;

    @Inject
    @BindToRegistry("quarkusCqlSession")
    QuarkusCqlSession session;

    @Override
    public void configure() throws Exception {
        from("direct:create")
                .toF("cql://%s/%s?username=%s&password=%s&cql=INSERT INTO employee (id, name, address) VALUES (?, ?, ?)", dbUrl,
                        KEYSPACE, userName, password);

        from("direct:createIdempotent")
                .idempotentConsumer(simple("${body[0]}"), new NamedCassandraIdempotentRepository(session, "ID"))
                .toF("cql://%s/%s?username=%s&password=%s&cql=INSERT INTO employee (id, name, address) VALUES (?, ?, ?)", dbUrl,
                        KEYSPACE, userName, password);

        from("direct:createCustomSession")
                .toF("cql:bean:customCqlSession?cql=INSERT INTO employee (id, name, address) VALUES (?, ?, ?)");

        from("direct:createQuarkusSession")
                .to("cql:bean:quarkusCqlSession?cql=INSERT INTO employee (id, name, address) VALUES (?, ?, ?)");

        from("direct:createCustomLoadBalancingPolicy")
                .toF("cql://%s/%s?username=%s&password=%s&loadBalancingPolicyClass=%s&cql=INSERT INTO employee (id, name, address) VALUES (?, ?, ?)",
                        dbUrl, KEYSPACE, userName, password, CustomLoadBalancingPolicy.class.getName());

        from("direct:read")
                .toF("cql://%s/%s?username=%s&password=%s&cql=SELECT * FROM employee WHERE id = ?", dbUrl, KEYSPACE, userName,
                        password);

        from("direct:update")
                .toF("cql://%s/%s?username=%s&password=%s&cql=UPDATE employee SET name = ?, address = ? WHERE id = ?", dbUrl,
                        KEYSPACE, userName, password);

        from("direct:delete")
                .toF("cql://%s/%s?username=%s&password=%s&cql=DELETE FROM employee WHERE id = ?", dbUrl, KEYSPACE, userName,
                        password);

        fromF("cql://%s/%s?username=%s&password=%s&repeatCount=1&cql=SELECT * FROM employee", dbUrl, KEYSPACE, userName,
                password).id("employee-consumer")
                        .autoStartup(false)
                        .to("seda:employees");

        from("direct:aggregate")
                .aggregate(simple("${body.id}"), createAggregationStrategy())
                .completionSize(3)
                .completionTimeout(5000)
                .aggregationRepository(createAggregationRepository())
                .to("seda:employees");

        from("direct:readWithCustomStrategy")
                .toF("cql://%s/%s?username=%s&password=%s&resultSetConversionStrategy=#customResultSetConversionStrategy&cql=SELECT * FROM employee WHERE id = ?",
                        dbUrl, KEYSPACE, userName, password);

        from("direct:cqlHeaderQuery")
                .toF("cql://%s/%s?username=%s&password=%s", dbUrl, KEYSPACE, userName, password);
    }

    private AggregationStrategy createAggregationStrategy() {
        return (oldExchange, newExchange) -> {
            if (oldExchange == null) {
                return newExchange;
            }
            Employee newBody = newExchange.getMessage().getBody(Employee.class);
            Object oldBody = oldExchange.getMessage().getBody();

            String newName = null;
            if (oldBody instanceof Employee) {
                newName = ((Employee) oldBody).getName() + "," + newBody.getName();
            } else if (oldBody instanceof String) {
                newName = oldBody + "," + newBody.getName();
            }

            oldExchange.getMessage().setBody(newName);
            return oldExchange;
        };
    }

    private AggregationRepository createAggregationRepository() {
        CassandraAggregationRepository repository = new NamedCassandraAggregationRepository();
        repository.setSession(session);
        return repository;
    }
}
