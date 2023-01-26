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
package org.apache.camel.quarkus.component.ognl.deployment;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;
import org.apache.camel.language.ognl.RootObject;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

class OgnlProcessor {
    private static final String FEATURE = "camel-ognl";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep(onlyIf = NativeBuild.class)
    void registerReflectiveClasses(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        IndexView view = combinedIndexBuildItem.getIndex();
        Set<Class<?>> types = new HashSet<>();
        types.add(RootObject.class);
        for (Method method : RootObject.class.getMethods()) {
            if (!method.getDeclaringClass().equals(Object.class)) {
                Class<?> returnType = method.getReturnType();
                if (types.add(returnType) && returnType.getPackageName().equals("org.apache.camel")) {
                    reflectiveClass.produce(
                            new ReflectiveClassBuildItem(
                                    false, true, false,
                                    view.getAllKnownImplementors(returnType).stream().map(ClassInfo::name)
                                            .map(DotName::toString).toArray(String[]::new)));
                }
            }
        }
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, true, false, types.toArray(new Class<?>[0])));
    }
}
