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
package org.apache.camel.quarkus.component.slack.graal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import com.oracle.svm.core.annotate.TargetElement;
import com.slack.api.SlackConfig;
import com.slack.api.audit.AuditConfig;
import com.slack.api.methods.MethodsConfig;
import com.slack.api.rate_limits.metrics.MetricsDatastore;
import com.slack.api.scim.SCIMConfig;
import com.slack.api.util.http.listener.DetailedLoggingListener;
import com.slack.api.util.http.listener.HttpResponseListener;
import com.slack.api.util.http.listener.ResponsePrettyPrintingListener;

import static com.oracle.svm.core.annotate.RecomputeFieldValue.Kind.Reset;

/**
 * Avoids original impl of eagerly initialized static fields which leads to a
 * started thread via com.slack.api.rate_limits.metrics.impl.BaseMemoryMetricsDatastore.MaintenanceJob
 */
final class SlackSubstitutions {
}

@TargetClass(SlackConfig.class)
final class SlackConfigSubstitutions {

    @Alias
    @RecomputeFieldValue(kind = Reset)
    private AuditConfig auditConfig = new AuditConfig() {
        void throwException() {
            throw new UnsupportedOperationException("This config is immutable");
        }

        @Override
        public void setStatsEnabled(boolean statsEnabled) {
            throwException();
        }

        @Override
        public void setExecutorName(String executorName) {
            throwException();
        }

        @Override
        public void setMaxIdleMills(int maxIdleMills) {
            throwException();
        }

        @Override
        public void setDefaultThreadPoolSize(int defaultThreadPoolSize) {
            throwException();
        }

        @Override
        public void setMetricsDatastore(MetricsDatastore metricsDatastore) {
            throwException();
        }

        @Override
        public void setCustomThreadPoolSizes(Map<String, Integer> customThreadPoolSizes) {
            throwException();
        }
    };

    @Alias
    @RecomputeFieldValue(kind = Reset)
    private MethodsConfig methodsConfig = new MethodsConfig() {
        void throwException() {
            throw new UnsupportedOperationException("This config is immutable");
        }

        @Override
        public void setStatsEnabled(boolean statsEnabled) {
            throwException();
        }

        @Override
        public void setExecutorName(String executorName) {
            throwException();
        }

        @Override
        public void setMaxIdleMills(int maxIdleMills) {
            throwException();
        }

        @Override
        public void setDefaultThreadPoolSize(int defaultThreadPoolSize) {
            throwException();
        }

        @Override
        public void setMetricsDatastore(MetricsDatastore metricsDatastore) {
            throwException();
        }

        @Override
        public void setCustomThreadPoolSizes(Map<String, Integer> customThreadPoolSizes) {
            throwException();
        }
    };

    @Alias
    @RecomputeFieldValue(kind = Reset)
    private SCIMConfig sCIMConfig = new SCIMConfig() {
        void throwException() {
            throw new UnsupportedOperationException("This config is immutable");
        }

        @Override
        public void setStatsEnabled(boolean statsEnabled) {
            throwException();
        }

        @Override
        public void setExecutorName(String executorName) {
            throwException();
        }

        @Override
        public void setMaxIdleMills(int maxIdleMills) {
            throwException();
        }

        @Override
        public void setDefaultThreadPoolSize(int defaultThreadPoolSize) {
            throwException();
        }

        @Override
        public void setMetricsDatastore(MetricsDatastore metricsDatastore) {
            throwException();
        }

        @Override
        public void setCustomThreadPoolSizes(Map<String, Integer> customThreadPoolSizes) {
            throwException();
        }
    };

    @Alias
    private List<HttpResponseListener> httpClientResponseHandlers = new ArrayList();

    @Substitute
    @TargetElement(name = "SlackConfig")
    public SlackConfigSubstitutions() {
        httpClientResponseHandlers.add(new DetailedLoggingListener());
        httpClientResponseHandlers.add(new ResponsePrettyPrintingListener());
    }
}

@TargetClass(AuditConfig.class)
final class AuditConfigSubstitutions {

    @Alias
    @RecomputeFieldValue(kind = Reset)
    public static AuditConfig DEFAULT_SINGLETON = null;
}

@TargetClass(MethodsConfig.class)
final class MethodsConfigSubstitutions {

    @Alias
    @RecomputeFieldValue(kind = Reset)
    public static MethodsConfig DEFAULT_SINGLETON = null;
}

@TargetClass(SCIMConfig.class)
final class SCIMConfigSubstitutions {

    @Alias
    @RecomputeFieldValue(kind = Reset)
    public static SCIMConfig DEFAULT_SINGLETON = null;
}
