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
package org.apache.camel.quarkus.core.graal;

import java.io.IOException;
import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.RecomputeFieldValue.Kind;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.apache.camel.impl.debugger.DebuggerJmxConnectorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Disable MBean server interactions in DebuggerJmxConnectorService if camel-management is not present.
 */
@TargetClass(value = DebuggerJmxConnectorService.class, onlyWith = CamelManagementAbsent.class)
final class DebuggerJmxConnectorServiceSubstitutions {
    @Alias
    @RecomputeFieldValue(kind = Kind.FromAlias)
    private static Logger LOG = LoggerFactory.getLogger(DebuggerJmxConnectorService.class);

    @Substitute
    protected void doStart() throws Exception {
        LOG.warn(
                "The JmxConnectorService is enabled but camel-quarkus-management is not detected. DebuggerJmxConnectorService will not be started.");
    }

    @Substitute
    protected void doStop() throws Exception {
        // Noop
    }

    @Substitute
    protected void createJmxConnector(String host) throws IOException {
        // Noop
    }
}

final class CamelManagementAbsent implements BooleanSupplier {
    @Override
    public boolean getAsBoolean() {
        try {
            Thread.currentThread().getContextClassLoader().loadClass("org.apache.camel.management.ManagedCamelContextImpl");
            return false;
        } catch (ClassNotFoundException e) {
            return true;
        }
    }
}
