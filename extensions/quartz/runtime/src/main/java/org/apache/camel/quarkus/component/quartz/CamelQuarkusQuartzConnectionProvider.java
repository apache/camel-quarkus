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
package org.apache.camel.quarkus.component.quartz;

import java.sql.Connection;
import java.sql.SQLException;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource.DataSourceLiteral;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import org.quartz.utils.ConnectionProvider;

public class CamelQuarkusQuartzConnectionProvider implements ConnectionProvider {
    private AgroalDataSource dataSource;
    private String dataSourceName;

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void shutdown() {
        // Do nothing as the connection will be closed inside the Agroal extension
    }

    @Override
    public void initialize() {
        final ArcContainer container = Arc.container();
        final InstanceHandle<AgroalDataSource> instanceHandle;
        final boolean useDefaultDataSource = dataSourceName == null || "".equals(dataSourceName.trim());
        if (useDefaultDataSource) {
            instanceHandle = container.instance(AgroalDataSource.class);
        } else {
            instanceHandle = container.instance(AgroalDataSource.class, new DataSourceLiteral(dataSourceName));
        }
        if (instanceHandle.isAvailable()) {
            this.dataSource = instanceHandle.get();
        } else {
            String message = String.format(
                    "JDBC Store configured but '%s' datasource is missing. You can configure your datasource by following the guide available at: https://quarkus.io/guides/datasource",
                    useDefaultDataSource ? "default" : dataSourceName);
            throw new IllegalStateException(message);
        }
    }

    public void setDataSourceName(String dataSourceName) {
        this.dataSourceName = dataSourceName;
    }
}
