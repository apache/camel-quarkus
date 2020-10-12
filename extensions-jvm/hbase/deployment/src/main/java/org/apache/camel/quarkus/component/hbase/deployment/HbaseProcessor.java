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
package org.apache.camel.quarkus.component.hbase.deployment;

import java.util.stream.Stream;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import org.apache.hadoop.hbase.client.backoff.ClientBackoffPolicyFactory.NoBackoffPolicy;
import org.apache.hadoop.hbase.security.UserProvider;
import org.apache.hadoop.security.JniBasedUnixGroupsMapping;

class HbaseProcessor {

    private static final String FEATURE = "camel-hbase";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void initAtRuntime(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClasses) {
        runtimeInitializedClasses.produce(new RuntimeInitializedClassBuildItem(JniBasedUnixGroupsMapping.class.getName()));
    }

    @BuildStep
    void nativeResources(BuildProducer<NativeImageResourceBuildItem> nativeResources) {
        Stream.of("core-default.xml", "core-site.xml", "hbase-default.xml", "hbase-site.xml")
                .map(NativeImageResourceBuildItem::new)
                .forEach(nativeResources::produce);
    }

    @BuildStep
    void refelectiveClasses(BuildProducer<ReflectiveClassBuildItem> refelectiveClasses) {
        refelectiveClasses.produce(new ReflectiveClassBuildItem(false, false,
                UserProvider.class.getName(),
                "org.apache.hadoop.hbase.client.ConnectionImplementation",
                NoBackoffPolicy.class.getName(),
                "org.apache.hadoop.hbase.client.SimpleRequestController",
                "org.apache.hadoop.hbase.client.ClusterStatusListener$MulticastListener",
                "org.apache.hadoop.hbase.client.ZKAsyncRegistry",
                "org.apache.hadoop.hbase.ipc.NettyRpcClient",
                "org.apache.zookeeper.ClientCnxnSocketNIO",
                "org.apache.hadoop.hbase.codec.KeyValueCodec",
                "sun.security.provider.ConfigFile"));
        refelectiveClasses.produce(new ReflectiveClassBuildItem(true, false,
                "com.sun.security.auth.module.UnixLoginModule",
                "org.apache.hadoop.security.UserGroupInformation$HadoopLoginModule"));
    }

}
