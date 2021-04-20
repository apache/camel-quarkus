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
package org.apache.camel.quarkus.component.mybatis.deployment;

import java.util.List;
import java.util.function.Supplier;

import io.quarkiverse.mybatis.deployment.SqlSessionFactoryBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.runtime.configuration.ConfigurationException;
import org.apache.camel.component.mybatis.MyBatisBeanComponent;
import org.apache.camel.component.mybatis.MyBatisComponent;
import org.apache.camel.quarkus.component.mybatis.runtime.CamelMyBatisRecorder;
import org.apache.camel.quarkus.core.deployment.spi.CamelBeanBuildItem;
import org.jboss.logging.Logger;

class MybatisProcessor {

    private static final Logger LOG = Logger.getLogger(MybatisProcessor.class);
    private static final String FEATURE = "camel-mybatis";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    CamelBeanBuildItem mybatisComponent(
            List<SqlSessionFactoryBuildItem> sqlSessionFactoryBuildItems, CamelMyBatisRecorder recorder) throws Throwable {
        return new CamelBeanBuildItem("mybatis", MyBatisComponent.class.getName(),
                recorder.createMyBatisComponent(
                        findXmlSqlSessionFactory(sqlSessionFactoryBuildItems).getSqlSessionFactory()));
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    CamelBeanBuildItem mybatisBeanComponent(
            List<SqlSessionFactoryBuildItem> sqlSessionFactoryBuildItems, CamelMyBatisRecorder recorder) throws Throwable {
        return new CamelBeanBuildItem("mybatis-bean", MyBatisBeanComponent.class.getName(),
                recorder.createMyBatisBeanComponent(
                        findXmlSqlSessionFactory(sqlSessionFactoryBuildItems).getSqlSessionFactory()));
    }

    private SqlSessionFactoryBuildItem findXmlSqlSessionFactory(
            List<SqlSessionFactoryBuildItem> sqlSessionFactoryBuildItems) throws Throwable {
        return sqlSessionFactoryBuildItems.stream().filter(s -> s.isFromXmlConfig()).findFirst()
                .orElseThrow((Supplier<Throwable>) () -> new ConfigurationException("No MyBatis XML Config"));
    }
}
