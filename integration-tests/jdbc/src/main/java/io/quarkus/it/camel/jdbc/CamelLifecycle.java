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
package io.quarkus.it.camel.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.sql.DataSource;

import org.jboss.logging.Logger;

import io.quarkus.camel.core.runtime.CamelRuntime;
import io.quarkus.camel.core.runtime.InitializingEvent;
import io.quarkus.camel.core.runtime.StartingEvent;

@ApplicationScoped
public class CamelLifecycle {

    private static final Logger log = Logger.getLogger(CamelLifecycle.class);

    @Inject
    CamelRuntime runtime;

    @Inject
    DataSource dataSource;

    public void initializing(@Observes InitializingEvent event) {
        log.debug("Binding camelsDs");
        runtime.getRegistry().bind("camelsDs", dataSource);
    }

    public void starting(@Observes StartingEvent event) throws SQLException {
        log.debug("Initializing camels table");
        try (Connection con = dataSource.getConnection()) {
            try (Statement statement = con.createStatement()) {
                try {
                    statement.execute("drop table camels");
                } catch (Exception ignored) {
                }
                statement.execute("create table camels (id int primary key, species varchar(255))");
                statement.execute("insert into camels (id, species) values (1, 'Camelus dromedarius')");
                statement.execute("insert into camels (id, species) values (2, 'Camelus bactrianus')");
                statement.execute("insert into camels (id, species) values (3, 'Camelus ferus')");
            }
            log.info("Initialized camels table");
        }
    }

}
