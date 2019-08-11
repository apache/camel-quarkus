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
package org.apache.camel.quarkus.core.runtime.graal;

import java.util.Map;
import java.util.function.Function;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.TargetClass;
import org.apache.camel.model.LoadBalancerDefinition;
import org.apache.camel.quarkus.core.runtime.support.Flags;
import org.apache.camel.reifier.loadbalancer.LoadBalancerReifier;

@TargetClass(className = "org.apache.camel.reifier.loadbalancer.LoadBalancerReifier", onlyWith = Flags.InitAtBuildTime.class)
final class Target_org_apache_camel_reifier_loadbalancer_LoadBalancerReifier {
    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    private static Map<Class<?>, Function<LoadBalancerDefinition, LoadBalancerReifier<? extends LoadBalancerDefinition>>> LOAD_BALANCERS = null;
}
