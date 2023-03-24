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
package org.apache.camel.quarkus.component.disruptor.deployment;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeReinitializedClassBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelServiceFilter;
import org.apache.camel.quarkus.core.deployment.spi.CamelServiceFilterBuildItem;

class DisruptorProcessor {
    private static final String FEATURE = "camel-disruptor";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    CamelServiceFilterBuildItem excludeDisruptorVM() {
        // The disruptor-vm provide support for communication across CamelContext instances but
        // since camel-quarkus support a single CamelContext, the component does not make sense.
        return new CamelServiceFilterBuildItem(CamelServiceFilter.forComponent("disruptor-vm"));
    }

    @BuildStep
    ReflectiveClassBuildItem reflectiveClasses(CombinedIndexBuildItem index) {
        // Note: this should be kept in sink with org.apache.camel.component.disruptor.DisruptorWaitStrategy
        return ReflectiveClassBuildItem.builder(BlockingWaitStrategy.class,
                SleepingWaitStrategy.class,
                BusySpinWaitStrategy.class,
                YieldingWaitStrategy.class).methods().build();
    }

    @BuildStep
    RuntimeReinitializedClassBuildItem reinitializedRingBufferFields() {
        // The `com.lmax.disruptor.RingBufferFields` class uses sun.misc.Unsafe behind the scenes to compute some static
        // fields and that confuses graalvm which emits warnings like:
        //
        //   Warning: RecomputeFieldValue.ArrayBaseOffset automatic substitution failed. The automatic substitution
        //   registration was attempted because a call to jdk.internal.misc.Unsafe.arrayBaseOffset(Class) was detected
        //   in the static initializer of com.lmax.disruptor.RingBufferFields. Detailed failure reason(s): Could not
        //   determine the field where the value produced by the call to jdk.internal.misc.Unsafe.arrayBaseOffset(Class)
        //   for the array base offset computation is stored. The call is not directly followed by a field store or by
        //   a sign extend node followed directly by a field store.
        //
        // Even if this is reported as a warning and the native compilation succeed, some static field are not computed
        // properly which result in weird result as runtime. For such reason, the static init method need to re-run at
        // runtime.
        return new RuntimeReinitializedClassBuildItem("com.lmax.disruptor.RingBufferFields");
    }
}
