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
package org.apache.camel.quarkus.support.mongodb.deployment;

import java.util.List;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.mongodb.deployment.MongoClientBuildItem;
import io.quarkus.mongodb.runtime.MongoClientBeanUtil;
import org.apache.camel.quarkus.core.deployment.spi.CamelRuntimeBeanBuildItem;

class SupportMongoDBProcessor {

    @BuildStep
    void registerCamelMongoClientProducers(
            List<MongoClientBuildItem> mongoClients,
            BuildProducer<CamelRuntimeBeanBuildItem> runtimeBeans) {

        for (MongoClientBuildItem mongoClient : mongoClients) {
            String clientName = getMongoClientName(mongoClient.getName());
            runtimeBeans.produce(
                    new CamelRuntimeBeanBuildItem(
                            clientName,
                            "com.mongodb.client.MongoClient",
                            mongoClient.getClient()));
        }
    }

    private String getMongoClientName(String clientName) {
        // Use the default mongo client instance name if it is the default connection
        return MongoClientBeanUtil.isDefault(clientName) ? "camelMongoClient" : clientName;
    }

}
