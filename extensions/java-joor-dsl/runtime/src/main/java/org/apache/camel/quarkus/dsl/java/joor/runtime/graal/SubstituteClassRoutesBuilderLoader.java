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
package org.apache.camel.quarkus.dsl.java.joor.runtime.graal;

import java.util.Collection;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.dsl.java.joor.ClassRoutesBuilderLoader;
import org.apache.camel.spi.Resource;

@TargetClass(ClassRoutesBuilderLoader.class)
final class SubstituteClassRoutesBuilderLoader {

    @Substitute
    protected Collection<RoutesBuilder> doLoadRoutesBuilders(Collection<Resource> resources) {
        throw new UnsupportedOperationException("The class files are not supported");
    }
}
