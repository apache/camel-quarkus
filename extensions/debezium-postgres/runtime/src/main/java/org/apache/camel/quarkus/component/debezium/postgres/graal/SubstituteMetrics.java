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
package org.apache.camel.quarkus.component.debezium.postgres.graal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.slf4j.Logger;

@TargetClass(io.debezium.metrics.Metrics.class)
final class SubstituteMetrics {

    @Substitute
    public synchronized void register(Logger logger) {
        //JMX is not supported in the native mode
        //because there is no API for avoiding MBean registration, substitution is used to skip registration
        // enhancement in debezium:https://issues.redhat.com/browse/DBZ-2089
        logger.warn("Metrics are not registered in native mode.");
    }

    @Substitute
    public final void unregister(Logger logger) {
        logger.debug("Metrics are not unregistered in native mode.");
    }
}
