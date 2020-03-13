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
package org.apache.camel.quarkus.caffeine.lrucache.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;

class CaffeineLRUCacheProcessor {
    private static final String FEATURE = "caffeine-lrucache";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    NativeImageResourceBuildItem lruCacheFactory() {
        // The process of discovering the LRUCacheFactory does not use any Camel' built in mechanic
        // but it based on loading a resource from the classpath, like:
        //
        //     ClassLoader classLoader = LRUCacheFactory.class.getClassLoader();
        //     URL url = classLoader.getResource("META-INF/services/org/apache/camel/lru-cache-factory");
        //
        // Full code here:
        //     https://github.com/apache/camel/blob/8bf781197c7138be5f8293e149a4a46a612cc40c/core/camel-support/src/main/java/org/apache/camel/support/LRUCacheFactory.java#L73-L100
        //
        // For such reason we need to include the lru-cache-factory file in the native image
        return new NativeImageResourceBuildItem("META-INF/services/org/apache/camel/lru-cache-factory");
    }

}
