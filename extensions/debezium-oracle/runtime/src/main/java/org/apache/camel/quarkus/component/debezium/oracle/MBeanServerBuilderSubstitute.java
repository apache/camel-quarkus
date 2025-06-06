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
package org.apache.camel.quarkus.component.debezium.oracle;

import javax.management.MBeanServer;
import javax.management.MBeanServerBuilder;
import javax.management.MBeanServerDelegate;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Substitution does not work and even though substituted method is called in the stack trace,
 * Quarkus detects, that MBeanServer is created. Another workaround with PortableMBeanFactory is provided.
 * This class is kept here for the future investigation.
 */
@TargetClass(MBeanServerBuilder.class)
final class MBeanServerBuilderSubstitute {

    @Substitute
    public MBeanServer newMBeanServer(String defaultDomain,
            MBeanServer outer,
            MBeanServerDelegate delegate) {
        // Not supported in native mode
        return null;
    }
}
