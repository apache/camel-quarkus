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
package org.apache.camel.quarkus.component.disruptor.graal;

import java.lang.reflect.Method;

import com.lmax.disruptor.util.ThreadHints;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import com.oracle.svm.core.jdk.JDK11OrLater;
import com.oracle.svm.core.jdk.JDK8OrEarlier;

public final class ThreadHintsSubstitutions {

    @TargetClass(value = ThreadHints.class, onlyWith = JDK8OrEarlier.class)
    @Substitute
    public final static class ThreadHints_JDK8OrEarlier {
        @Substitute
        public static void onSpinWait() {
            // do nothing since onSpinWait is available in Java >= 9
        }
    }

    @TargetClass(value = ThreadHints.class, onlyWith = JDK11OrLater.class)
    @Substitute
    public final static class ThreadHints_JDK11OrLater {
        private static final Method ON_SPIN_WAIT_METHOD = lookupOnSpinWait();

        @Substitute
        public static void onSpinWait() {
            if (null != ON_SPIN_WAIT_METHOD) {
                try {
                    ON_SPIN_WAIT_METHOD.invoke(null);
                } catch (final Throwable ignore) {
                }
            }
        }

        public static Method lookupOnSpinWait() {
            try {
                // use old good Class.getMethod as the method handle technique used by
                // ThreadHints seems to be one of the cases not covered by SubstrateVM
                return Thread.class.getMethod("onSpinWait");
            } catch (Exception ignore) {
            }

            return null;
        }
    }
}
