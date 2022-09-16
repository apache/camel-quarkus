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
package org.apache.camel.quarkus.support.jdbc.deployment;

import java.util.List;

import javax.sql.DataSource;

import io.quarkus.agroal.spi.JdbcDataSourceBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.runtime.RuntimeValue;
import org.apache.camel.quarkus.core.CamelBeanQualifierResolver;
import org.apache.camel.quarkus.core.deployment.spi.CamelBeanQualifierResolverBuildItem;
import org.apache.camel.quarkus.support.jdbc.JdbcRecorder;

public class JdbcSupportProcessor {

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void registerNamedDataSourceCamelBeanQualifierResolver(
            List<JdbcDataSourceBuildItem> dataSources,
            BuildProducer<CamelBeanQualifierResolverBuildItem> camelBeanQualifierResolver,
            JdbcRecorder recorder) {

        // If there are multiple DataSource configs, then users need to explicitly state which one to use
        // via their component / endpoint configuration. Otherwise if there is just 1, and it is not the default DataSource,
        // we can create a resolver for DataSourceLiteral and make named DataSource autowiring work as expected
        if (dataSources.size() == 1) {
            JdbcDataSourceBuildItem dataSource = dataSources.get(0);
            if (!dataSource.isDefault()) {
                RuntimeValue<CamelBeanQualifierResolver> runtimeValue = recorder
                        .createDataSourceQualifierResolver(dataSource.getName());
                CamelBeanQualifierResolverBuildItem beanQualifierResolver = new CamelBeanQualifierResolverBuildItem(
                        DataSource.class, runtimeValue);
                camelBeanQualifierResolver.produce(beanQualifierResolver);
            }
        }
    }
}
