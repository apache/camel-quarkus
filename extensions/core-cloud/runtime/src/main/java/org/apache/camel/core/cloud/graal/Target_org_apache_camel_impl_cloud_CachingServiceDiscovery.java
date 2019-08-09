/**
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
package org.apache.camel.core.cloud.graal;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.apache.camel.cloud.ServiceDefinition;
import org.apache.camel.cloud.ServiceDiscovery;

@TargetClass(className = "org.apache.camel.impl.cloud.CachingServiceDiscovery")
final class Target_org_apache_camel_impl_cloud_CachingServiceDiscovery {
    @Alias
    private ServiceDiscovery delegate;
    @Alias
    private LoadingCache<String, List<ServiceDefinition>> cache;
    @Alias
    private long timeout;

    @Substitute
    public void setTimeout(long timeout) {
        this.timeout = timeout;
        this.cache = Caffeine.newBuilder()
            .executor(new Executor() {
                @Override
                public void execute(Runnable command) {
                    // workaround for https://github.com/quarkusio/quarkus/issues/3300
                    ForkJoinPool.commonPool().execute(command);
                }
            })
            .expireAfterAccess(timeout, TimeUnit.MILLISECONDS)
            .build(delegate::getServices);
    }
}
