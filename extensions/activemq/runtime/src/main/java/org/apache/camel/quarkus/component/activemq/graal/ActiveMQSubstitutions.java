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
package org.apache.camel.quarkus.component.activemq.graal;

import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.apache.activemq.filter.BooleanExpression;
import org.apache.activemq.filter.UnaryExpression;
import org.apache.activemq.filter.XPathExpression;

final class ActiveMQSubstitutions {
}

@TargetClass(XPathExpression.class)
@Delete
final class SubstituteXPathExpression {
}

@TargetClass(UnaryExpression.class)
final class SubstituteUnaryExpression {

    @Substitute
    public static BooleanExpression createXPath(final String xpath) {
        // The required dependencies to make this work are not on the classpath by default
        // Since this appears to be a somewhat niche feature for Camel, it is not supported in native mode
        throw new RuntimeException("XPath selectors are not supported in native mode");
    }
}
