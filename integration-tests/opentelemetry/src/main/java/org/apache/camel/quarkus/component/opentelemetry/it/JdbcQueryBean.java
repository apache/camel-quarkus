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
package org.apache.camel.quarkus.component.opentelemetry.it;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import io.agroal.api.AgroalDataSource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Singleton
@Named("jdbcQueryBean")
@RegisterForReflection(fields = false)
public class JdbcQueryBean {
    @Inject
    AgroalDataSource dataSource;

    public long getNowTimestamp() {
        try (Connection connection = dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery("SELECT EXTRACT(EPOCH FROM NOW());");
                if (resultSet.next()) {
                    return resultSet.getLong(1);
                }
                return 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
