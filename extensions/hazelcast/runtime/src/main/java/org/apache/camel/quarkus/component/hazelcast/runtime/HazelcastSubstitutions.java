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
package org.apache.camel.quarkus.component.hazelcast.runtime;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

public class HazelcastSubstitutions {
}

/**
 * Force usage of Hazelcast client
 */
@TargetClass(Hazelcast.class)
final class Target_Hazelcast {
    @Substitute
    public static HazelcastInstance newHazelcastInstance(Config config) {
        throw new UnsupportedOperationException(
                "Hazelcast node mode is not supported. Please use client mode.");
    }

    @Substitute
    public static HazelcastInstance getOrCreateHazelcastInstance(Config config) {
        throw new UnsupportedOperationException(
                "Hazelcast node mode is not supported. Please use client mode.");
    }
}
