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
package org.apache.camel.quarkus.component.cassandraql.it;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.datastax.oss.driver.api.core.context.DriverContext;
import com.datastax.oss.driver.api.core.metadata.Node;
import com.datastax.oss.driver.internal.core.loadbalancing.DefaultLoadBalancingPolicy;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection(fields = false, methods = false)
public class CustomLoadBalancingPolicy extends DefaultLoadBalancingPolicy {
    private static final CountDownLatch latch = new CountDownLatch(1);

    public CustomLoadBalancingPolicy(@NonNull DriverContext context, @NonNull String profileName) {
        super(context, profileName);
    }

    @Override
    public void init(@NonNull Map<UUID, Node> nodes, @NonNull DistanceReporter distanceReporter) {
        super.init(nodes, distanceReporter);
        latch.countDown();
    }

    public static boolean awaitInitialization() throws InterruptedException {
        return latch.await(5, TimeUnit.SECONDS);
    }
}
