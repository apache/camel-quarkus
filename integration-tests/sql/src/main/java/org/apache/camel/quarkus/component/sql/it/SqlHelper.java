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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.microprofile.config.ConfigProvider;

public class SqlHelper {

    private static Set<String> BOOLEAN_AS_NUMBER = new HashSet<>(Arrays.asList("db2", "mssql", "oracle"));

    static String convertBooleanToSqlDialect(String dbKind, boolean value) {
        return convertBooleanToSqlResult(dbKind, value).toString();
    }

    static Object convertBooleanToSqlResult(String dbKind, boolean value) {

        if (value) {
            return BOOLEAN_AS_NUMBER.contains(dbKind) ? 1 : true;
        }
        return BOOLEAN_AS_NUMBER.contains(dbKind) ? 0 : false;
    }

    static String getSelectProjectsScriptName(String dbKind) {
        return BOOLEAN_AS_NUMBER.contains(dbKind) ? "selectProjectsAsNumber.sql" : "selectProjectsAsBoolean.sql";
    }

    public static boolean useDocker() {
        return Boolean.parseBoolean(System.getenv("SQL_USE_DERBY_DOCKER")) &&
                "derby".equals(ConfigProvider.getConfig().getOptionalValue("quarkus.datasource.db-kind", String.class)
                        .orElse(System.getProperty("cq.sqlJdbcKind")))
                && System.getenv("SQL_JDBC_URL") == null;
    }

}
