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
package org.apache.camel.quarkus.component.hazelcast.it;

import java.util.Map;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class HazelcastTestResource implements QuarkusTestResourceLifecycleManager {
    private volatile HazelcastInstance member;
    private static volatile HazelcastInstance member2;

    @Override
    public Map<String, String> start() {
        member = Hazelcast.newHazelcastInstance();
        return null;
    }

    @Override
    public void stop() {
        if (member != null) {
            member.shutdown();
        }

        if (member2 != null) {
            member.shutdown();
        }
    }

    /**
     * this is used to test new instance in the same cluster
     */
    public static void addMemberToCluster() {
        member2 = Hazelcast.newHazelcastInstance();
    }

}
