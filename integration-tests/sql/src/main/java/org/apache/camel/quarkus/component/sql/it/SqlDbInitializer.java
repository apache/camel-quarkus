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
package org.apache.camel.quarkus.component.sql.it;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.agroal.api.AgroalDataSource;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class SqlDbInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlDbInitializer.class);

    @Inject
    AgroalDataSource dataSource;

    @ConfigProperty(name = "quarkus.datasource.db-kind")
    String dbKind;

    public void initDb() throws SQLException, IOException {

        try (Connection conn = dataSource.getConnection()) {
            try (Statement statement = conn.createStatement()) {
                try (InputStream is = Thread.currentThread().getContextClassLoader()
                        .getResourceAsStream("sql/" + dbKind + "/initDb.sql");
                        InputStreamReader isr = new InputStreamReader(is);
                        BufferedReader reader = new BufferedReader(isr)) {

                    reader.lines().filter(s -> s != null && !"".equals(s) && !s.startsWith("--")).forEach(s -> {
                        try {
                            statement.execute(s);
                        } catch (SQLException e) {
                            if (!s.toUpperCase().startsWith("DROP TABLE")) {
                                throw new RuntimeException(e);
                            } else {
                                LOGGER.debug(String.format("Command '%s' failed.", s)); //use debug logging
                            }
                        }
                    });
                }
            }
        }

    }
}
