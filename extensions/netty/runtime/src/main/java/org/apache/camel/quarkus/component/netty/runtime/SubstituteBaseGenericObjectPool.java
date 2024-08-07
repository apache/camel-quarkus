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
package org.apache.camel.quarkus.component.netty.runtime;

import java.util.Arrays;
import java.util.function.BooleanSupplier;

import javax.management.ObjectName;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.apache.commons.pool2.impl.BaseGenericObjectPool;
import org.apache.commons.pool2.impl.BaseObjectPoolConfig;

/**
 * Disable commons-pool2 JMX operations. Only active if quarkus-pooled-jms is not on the classpath. Since it has
 * the same substitutions.
 */
@TargetClass(value = BaseGenericObjectPool.class, onlyWith = QuarkusPooledJmsAbsent.class)
final class SubstituteBaseGenericObjectPool {
    @Substitute
    private ObjectName jmxRegister(final BaseObjectPoolConfig<?> config, final String jmxNameBase, String jmxNamePrefix) {
        return null;
    }

    @Substitute
    void jmxUnregister() {
        // Not supported in native mode
    }
}

final class QuarkusPooledJmsAbsent implements BooleanSupplier {
    @Override
    public boolean getAsBoolean() {
        return Arrays.stream(Package.getPackages())
                .map(Package::getName)
                .noneMatch(p -> p.equals("io.quarkiverse.messaginghub.pooled.jms"));
    }
}
