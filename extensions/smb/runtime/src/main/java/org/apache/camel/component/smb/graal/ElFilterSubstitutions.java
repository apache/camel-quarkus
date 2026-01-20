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
package org.apache.camel.component.smb.graal;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import jakarta.el.ExpressionFactory;
import net.engio.mbassy.dispatch.el.StandardELResolutionContext;
import net.engio.mbassy.subscription.SubscriptionContext;

/**
 * Disables unwanted for smbj jakarta.el support in net.engio:mbassador
 *
 * TODO: Remove this class when mbassador > 1.3.0 is available - https://github.com/apache/camel-quarkus/issues/5646
 */
public class ElFilterSubstitutions {
    @TargetClass(className = "net.engio.mbassy.dispatch.el.ElFilter")
    static final class SubstituteElFilter {
        @Substitute
        public static boolean isELAvailable() {
            return false;
        }

        @Substitute
        public static ExpressionFactory ELFactory() {
            return null;
        }

        @Substitute
        public boolean accepts(Object message, final SubscriptionContext context) {
            return false;
        }

        @Substitute
        private boolean evalExpression(final String expression,
                final StandardELResolutionContext resolutionContext,
                final SubscriptionContext context,
                final Object message) {
            return false;
        }
    }

    @TargetClass(className = "net.engio.mbassy.dispatch.el.ElFilter$ExpressionFactoryHolder")
    static final class SubstituteExpressionFactoryHolder {
        @Alias
        @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
        public static ExpressionFactory ELFactory = null;
    }
}
