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
package org.apache.camel.quarkus.component.pgevent.deployment;

import java.io.IOException;
import java.sql.Driver;
import java.util.Set;
import java.util.stream.Stream;

import com.impossibl.postgres.system.procs.ProcProvider;
import io.quarkus.agroal.spi.JdbcDriverBuildItem;
import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.SslNativeConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.util.ServiceUtil;

class PgeventProcessor {

    private static final String PGEVENT_SERVICE_BASE = "META-INF/services/";

    private static final String FEATURE = "camel-pgevent";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ReflectiveClassBuildItem registerReflectiveClasses() {
        return ReflectiveClassBuildItem.builder("io.netty.channel.nio.NioEventLoopGroup").methods(false).fields(true).build();
    }

    @BuildStep
    void registerNativeImageResources(BuildProducer<ServiceProviderBuildItem> services) {
        Stream.of(
                ProcProvider.class.getName(),
                Driver.class.getName())
                .forEach(service -> {
                    try {
                        Set<String> implementations = ServiceUtil.classNamesNamedIn(
                                Thread.currentThread().getContextClassLoader(),
                                PGEVENT_SERVICE_BASE + service);
                        services.produce(
                                new ServiceProviderBuildItem(service,
                                        implementations.toArray(new String[0])));

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    /**
     * Setting Agroal Datasource driver for pgjdbc-ng driver
     *
     */
    @BuildStep
    void registerDriver(BuildProducer<JdbcDriverBuildItem> jdbcDriver,
            SslNativeConfigBuildItem sslNativeConfigBuildItem) {
        jdbcDriver.produce(new JdbcDriverBuildItem(DatabaseKind.POSTGRESQL, "com.impossibl.postgres.jdbc.PGDriver",
                "com.impossibl.postgres.jdbc.xa.PGXADataSource"));
    }

}
